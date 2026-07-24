package me.colinxu.randomoptimization;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FilePackResourcesIndex {
    private final ZipFile zipFile;
    private final ZipEntry[] entries;
    private final Map<String, PrefixIndex> indexesByPrefix = new HashMap<>();

    private FilePackResourcesIndex(ZipFile zipFile, ZipEntry[] entries) {
        this.zipFile = zipFile;
        this.entries = entries;
    }

    public static FilePackResourcesIndex build(ZipFile zipFile) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            entries.add(enumeration.nextElement());
        }
        return new FilePackResourcesIndex(zipFile, entries.toArray(ZipEntry[]::new));
    }

    public Set<String> getNamespaces(String packPrefix, PackType packType) {
        return this.getOrCreatePrefixIndex(packPrefix)
                .indexesByType[packType.ordinal()]
                .namespaces;
    }

    public void listResources(String packPrefix, PackType packType, String namespace, String path,
                              PackResources.ResourceOutput output) {
        TypeIndex typeIndex = this.getOrCreatePrefixIndex(packPrefix)
                .indexesByType[packType.ordinal()];
        NamespaceIndex namespaceIndex = typeIndex.resourcesByNamespace.get(namespace);
        if (namespaceIndex == null) {
            return;
        }

        IndexedResource[] resources = namespaceIndex.resourcesByDirectory.get(path);
        if (resources == null) {
            return;
        }

        for (IndexedResource resource : resources) {
            output.accept(resource.location, resource.supplier);
        }
    }

    private synchronized PrefixIndex getOrCreatePrefixIndex(String packPrefix) {
        return this.indexesByPrefix.computeIfAbsent(packPrefix, this::buildPrefixIndex);
    }

    private PrefixIndex buildPrefixIndex(String packPrefix) {
        PackType[] packTypes = PackType.values();
        TypeIndexBuilder[] builders = new TypeIndexBuilder[packTypes.length];
        String[] typePrefixes = new String[packTypes.length];

        for (int i = 0; i < packTypes.length; ++i) {
            builders[i] = new TypeIndexBuilder();
            String typeDirectory = packTypes[i].getDirectory() + "/";
            typePrefixes[i] = packPrefix.isEmpty()
                    ? typeDirectory
                    : packPrefix + "/" + typeDirectory;
        }

        for (ZipEntry entry : this.entries) {
            String entryName = entry.getName();
            for (int i = 0; i < typePrefixes.length; ++i) {
                String typePrefix = typePrefixes[i];
                if (entryName.startsWith(typePrefix)) {
                    builders[i].accept(this.zipFile, entry, entryName, typePrefix.length());
                    break;
                }
            }
        }

        TypeIndex[] indexesByType = new TypeIndex[packTypes.length];
        for (int i = 0; i < indexesByType.length; ++i) {
            indexesByType[i] = builders[i].build();
        }
        return new PrefixIndex(indexesByType);
    }

    private static final class TypeIndexBuilder {
        private final Set<String> namespaces = new HashSet<>();
        private final Map<String, NamespaceIndexBuilder> resourcesByNamespace = new HashMap<>();

        private void accept(ZipFile zipFile, ZipEntry entry, String entryName,
                            int namespaceStart) {
            int namespaceEnd = entryName.indexOf('/', namespaceStart);
            String namespace = namespaceEnd < 0
                    ? entryName.substring(namespaceStart)
                    : entryName.substring(namespaceStart, namespaceEnd);

            // This matches FilePackResources#getNamespaces: an entry directly at
            // assets/<namespace> also contributes a namespace.
            if (!namespace.isEmpty() && ResourceLocation.isValidNamespace(namespace)) {
                this.namespaces.add(namespace);
            }

            if (entry.isDirectory() || namespaceEnd < 0) {
                return;
            }

            String resourcePath = entryName.substring(namespaceEnd + 1);
            ResourceLocation location = ResourceLocation.tryBuild(namespace, resourcePath);
            if (location == null) {
                return;
            }

            NamespaceIndexBuilder namespaceBuilder = this.resourcesByNamespace
                    .computeIfAbsent(namespace, ignored -> new NamespaceIndexBuilder());
            namespaceBuilder.add(
                    resourcePath,
                    new IndexedResource(location, IoSupplier.create(zipFile, entry))
            );
        }

        private TypeIndex build() {
            Map<String, NamespaceIndex> namespaceIndexes =
                    new HashMap<>(capacityFor(this.resourcesByNamespace.size()));
            this.resourcesByNamespace.forEach((namespace, builder) ->
                    namespaceIndexes.put(namespace, builder.build()));
            return new TypeIndex(Set.copyOf(this.namespaces), Map.copyOf(namespaceIndexes));
        }
    }

    private static final class NamespaceIndexBuilder {
        private final Map<String, List<IndexedResource>> resourcesByDirectory = new HashMap<>();

        private void add(String resourcePath, IndexedResource resource) {
            // listResources is recursive. Index a/b/c.json under both "a" and
            // "a/b", preserving the ZIP enumeration order within each result.
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
            return new NamespaceIndex(Map.copyOf(directoryIndexes));
        }
    }

    private static int capacityFor(int expectedSize) {
        return expectedSize < 3 ? expectedSize + 1 : (int) (expectedSize / 0.75F) + 1;
    }

    private record PrefixIndex(TypeIndex[] indexesByType) {
    }

    private record TypeIndex(Set<String> namespaces,
                             Map<String, NamespaceIndex> resourcesByNamespace) {
    }

    private record NamespaceIndex(Map<String, IndexedResource[]> resourcesByDirectory) {
    }

    private record IndexedResource(ResourceLocation location,
                                   IoSupplier<InputStream> supplier) {
    }
}
