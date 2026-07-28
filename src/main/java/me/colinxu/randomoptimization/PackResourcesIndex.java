package me.colinxu.randomoptimization;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable lookup table shared by ZIP-backed and path-backed packs.
 */
public final class PackResourcesIndex {
    private final TypeIndex[] indexesByType;

    private PackResourcesIndex(TypeIndex[] indexesByType) {
        this.indexesByType = indexesByType;
    }

    public static Builder builder() {
        return new Builder(true);
    }

    public static Builder listingOnlyBuilder() {
        return new Builder(false);
    }

    public Set<String> getNamespaces(PackType packType) {
        return this.indexesByType[packType.ordinal()].namespaces;
    }

    @Nullable
    public IoSupplier<InputStream> getResource(
            PackType packType,
            ResourceLocation location
    ) {
        return this.indexesByType[packType.ordinal()]
                .resourcesByLocation
                .get(location);
    }

    public void listResources(
            PackType packType,
            String namespace,
            String path,
            PackResources.ResourceOutput output
    ) {
        NamespaceIndex namespaceIndex = this.indexesByType[packType.ordinal()]
                .resourcesByNamespace
                .get(namespace);
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

    public static final class Builder {
        private final TypeIndexBuilder[] builders;

        private Builder(boolean indexIndividualResources) {
            PackType[] packTypes = PackType.values();
            this.builders = new TypeIndexBuilder[packTypes.length];
            for (int i = 0; i < packTypes.length; ++i) {
                this.builders[i] =
                        new TypeIndexBuilder(indexIndividualResources);
            }
        }

        public void addNamespace(PackType packType, String namespace) {
            if (ResourceLocation.isValidNamespace(namespace)) {
                this.builders[packType.ordinal()].namespaces.add(namespace);
            }
        }

        /**
         * Adds a resource if no higher-priority path has already supplied it.
         */
        public void addResource(
                PackType packType,
                String namespace,
                String resourcePath,
                IoSupplier<InputStream> supplier
        ) {
            ResourceLocation location =
                    ResourceLocation.tryBuild(namespace, resourcePath);
            if (location == null) {
                return;
            }

            TypeIndexBuilder typeBuilder = this.builders[packType.ordinal()];
            typeBuilder.namespaces.add(namespace);
            if (typeBuilder.resourcesByLocation != null
                    && typeBuilder.resourcesByLocation.putIfAbsent(
                            location,
                            supplier
                    ) != null) {
                return;
            }

            typeBuilder.resourcesByNamespace
                    .computeIfAbsent(namespace, ignored -> new NamespaceIndexBuilder())
                    .add(resourcePath, new IndexedResource(location, supplier));
        }

        public PackResourcesIndex build() {
            TypeIndex[] indexes = new TypeIndex[this.builders.length];
            for (int i = 0; i < indexes.length; ++i) {
                indexes[i] = this.builders[i].build();
            }
            return new PackResourcesIndex(indexes);
        }
    }

    private static final class TypeIndexBuilder {
        private final Set<String> namespaces = new HashSet<>();
        @Nullable
        private final Map<ResourceLocation, IoSupplier<InputStream>>
                resourcesByLocation;
        private final Map<String, NamespaceIndexBuilder> resourcesByNamespace =
                new HashMap<>();

        private TypeIndexBuilder(boolean indexIndividualResources) {
            this.resourcesByLocation =
                    indexIndividualResources ? new HashMap<>() : null;
        }

        private TypeIndex build() {
            Map<String, NamespaceIndex> namespaceIndexes =
                    new HashMap<>(capacityFor(this.resourcesByNamespace.size()));
            this.resourcesByNamespace.forEach((namespace, builder) ->
                    namespaceIndexes.put(namespace, builder.build()));
            return new TypeIndex(
                    Set.copyOf(this.namespaces),
                    this.resourcesByLocation == null
                            ? Map.of()
                            : Map.copyOf(this.resourcesByLocation),
                    Map.copyOf(namespaceIndexes)
            );
        }
    }

    private static final class NamespaceIndexBuilder {
        private final Map<String, List<IndexedResource>> resourcesByDirectory =
                new HashMap<>();

        private void add(String resourcePath, IndexedResource resource) {
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
                    directoryIndexes.put(
                            directory,
                            resources.toArray(IndexedResource[]::new)
                    ));
            return new NamespaceIndex(Map.copyOf(directoryIndexes));
        }
    }

    private static int capacityFor(int expectedSize) {
        return expectedSize < 3
                ? expectedSize + 1
                : (int) (expectedSize / 0.75F) + 1;
    }

    private record TypeIndex(
            Set<String> namespaces,
            Map<ResourceLocation, IoSupplier<InputStream>> resourcesByLocation,
            Map<String, NamespaceIndex> resourcesByNamespace
    ) {
    }

    private record NamespaceIndex(
            Map<String, IndexedResource[]> resourcesByDirectory
    ) {
    }

    private record IndexedResource(
            ResourceLocation location,
            IoSupplier<InputStream> supplier
    ) {
    }
}
