package me.colinxu.randomoptimization.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Immutable lookup index for a {@link FilePackResources} ZIP.
 *
 * <p>This helper must stay outside the package declared by the Mixin configuration;
 * Mixin reserves that entire package tree for mixin classes.</p>
 */
public final class FilePackResourcesIndex {
    private final TypeIndex[] indexesByType;

    private FilePackResourcesIndex(TypeIndex[] indexesByType) {
        this.indexesByType = indexesByType;
    }

    public static FilePackResourcesIndex build(ZipFile zipFile) {
        PackType[] packTypes = PackType.values();
        TypeIndexBuilder[] builders = new TypeIndexBuilder[packTypes.length];
        String[] prefixes = new String[packTypes.length];

        for (int i = 0; i < packTypes.length; ++i) {
            builders[i] = new TypeIndexBuilder();
            prefixes[i] = packTypes[i].getDirectory() + '/';
        }

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // PackType currently has only assets and data. A tiny array scan avoids
            // allocating the first path component for every ZIP entry.
            for (int i = 0; i < prefixes.length; ++i) {
                String prefix = prefixes[i];
                if (entryName.startsWith(prefix)) {
                    builders[i].accept(zipFile, entry, entryName, prefix.length());
                    break;
                }
            }
        }

        TypeIndex[] indexes = new TypeIndex[packTypes.length];
        for (int i = 0; i < indexes.length; ++i) {
            indexes[i] = builders[i].build();
        }
        return new FilePackResourcesIndex(indexes);
    }

    public Set<String> getNamespaces(PackType packType) {
        return this.indexesByType[packType.ordinal()].namespaces;
    }

    public void listResources(PackType packType, String namespace, String path,
                              PackResources.ResourceOutput output) {
        this.listResources(packType, namespace, path, false, output);
    }

    public void listResources(PackType packType, String namespace, String path,
                              boolean normalizeArchivePrefix,
                              PackResources.ResourceOutput output) {
        NamespaceIndex namespaceIndex =
                this.indexesByType[packType.ordinal()].resourcesByNamespace.get(namespace);
        if (namespaceIndex == null) {
            return;
        }

        IndexedResource[] resources = normalizeArchivePrefix && path.isEmpty()
                ? namespaceIndex.resources
                : namespaceIndex.resourcesByDirectory.get(path);
        if (resources == null) {
            return;
        }

        for (IndexedResource resource : resources) {
            output.accept(resource.location, resource);
        }
    }

    private static final class TypeIndexBuilder {
        private final Set<String> namespaces = new HashSet<>();
        private final Map<String, NamespaceIndexBuilder> resourcesByNamespace = new HashMap<>();

        private void accept(ZipFile zipFile, ZipEntry entry, String entryName, int prefixLength) {
            int namespaceEnd = entryName.indexOf('/', prefixLength);
            String exactNamespace = namespaceEnd < 0
                    ? null
                    : entryName.substring(prefixLength, namespaceEnd);
            this.collectNamespace(entryName, prefixLength, exactNamespace);

            if (entry.isDirectory() || exactNamespace == null) {
                return;
            }

            String resourcePath = entryName.substring(namespaceEnd + 1);
            NamespaceIndexBuilder namespaceBuilder =
                    this.resourcesByNamespace.get(exactNamespace);
            String canonicalNamespace =
                    namespaceBuilder == null ? exactNamespace : namespaceBuilder.namespace;
            ResourceLocation location =
                    ResourceLocation.tryBuild(canonicalNamespace, resourcePath);
            if (location == null) {
                return;
            }

            if (namespaceBuilder == null) {
                namespaceBuilder = new NamespaceIndexBuilder(canonicalNamespace);
                this.resourcesByNamespace.put(canonicalNamespace, namespaceBuilder);
            }
            IndexedResource resource = new IndexedResource(location, zipFile, entry);
            namespaceBuilder.add(resourcePath, resource);
        }

        /**
         * Match FilePackResources' Splitter.on('/').omitEmptyStrings().limit(3)
         * namespace behavior without creating a splitter result list per ZIP entry.
         */
        private void collectNamespace(String entryName, int prefixLength, String exactNamespace) {
            int namespaceStart = prefixLength;
            while (namespaceStart < entryName.length()
                    && entryName.charAt(namespaceStart) == '/') {
                ++namespaceStart;
            }
            if (namespaceStart == entryName.length()) {
                return;
            }

            int namespaceEnd = entryName.indexOf('/', namespaceStart);
            if (namespaceEnd < 0) {
                namespaceEnd = entryName.length();
            }
            String namespace = namespaceStart == prefixLength && exactNamespace != null
                    ? exactNamespace
                    : entryName.substring(namespaceStart, namespaceEnd);
            if (isLowercase(namespace)) {
                this.namespaces.add(namespace);
            }
        }

        private TypeIndex build() {
            Map<String, NamespaceIndex> namespaceIndexes =
                    new HashMap<>(capacityFor(this.resourcesByNamespace.size()));
            this.resourcesByNamespace.forEach((namespace, builder) ->
                    namespaceIndexes.put(namespace, builder.build()));
            return new TypeIndex(Set.copyOf(this.namespaces), namespaceIndexes);
        }
    }

    private static final class NamespaceIndexBuilder {
        private final String namespace;
        private final List<IndexedResource> resources = new ArrayList<>();
        private final Map<String, List<IndexedResource>> resourcesByDirectory = new HashMap<>();

        private NamespaceIndexBuilder(String namespace) {
            this.namespace = namespace;
        }

        private void add(String resourcePath, IndexedResource resource) {
            this.resources.add(resource);

            // A listResources("models") request is recursive, so models/a/b.json is
            // indexed under both "models" and "models/a". Empty and trailing path
            // components are intentionally retained to match literal ZIP prefix behavior.
            int slash = resourcePath.indexOf('/');
            while (slash >= 0) {
                String directory = resourcePath.substring(0, slash);
                this.resourcesByDirectory
                        .computeIfAbsent(directory, ignored -> new ArrayList<>())
                        .add(resource);
                slash = resourcePath.indexOf('/', slash + 1);
            }
        }

        private NamespaceIndex build() {
            Map<String, IndexedResource[]> directoryIndexes =
                    new HashMap<>(capacityFor(this.resourcesByDirectory.size()));
            this.resourcesByDirectory.forEach((directory, resources) ->
                    directoryIndexes.put(directory, resources.toArray(IndexedResource[]::new)));
            return new NamespaceIndex(
                    this.resources.toArray(IndexedResource[]::new),
                    directoryIndexes
            );
        }
    }

    private static int capacityFor(int expectedSize) {
        return expectedSize < 3 ? expectedSize + 1 : (int) (expectedSize / 0.75F) + 1;
    }

    private static boolean isLowercase(String value) {
        for (int i = 0; i < value.length(); ++i) {
            char character = value.charAt(i);
            if (character >= 'A' && character <= 'Z') {
                return false;
            }
            if (character >= 128) {
                return value.equals(value.toLowerCase(Locale.ROOT));
            }
        }
        return true;
    }

    private record TypeIndex(Set<String> namespaces,
                             Map<String, NamespaceIndex> resourcesByNamespace) {
    }

    private record NamespaceIndex(
            IndexedResource[] resources,
            Map<String, IndexedResource[]> resourcesByDirectory
    ) {
    }

    /** One object serves as both the cached result record and its cached stream supplier. */
    private static final class IndexedResource implements IoSupplier<InputStream> {
        private final ResourceLocation location;
        private final ZipFile zipFile;
        private final ZipEntry zipEntry;

        private IndexedResource(ResourceLocation location, ZipFile zipFile, ZipEntry zipEntry) {
            this.location = location;
            this.zipFile = zipFile;
            this.zipEntry = zipEntry;
        }

        @Override
        public InputStream get() throws IOException {
            return this.zipFile.getInputStream(this.zipEntry);
        }
    }
}
