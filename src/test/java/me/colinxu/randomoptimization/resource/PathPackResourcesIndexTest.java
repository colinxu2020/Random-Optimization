package me.colinxu.randomoptimization.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathPackResourcesIndexTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void indexesNamespacesListingsAndResources() throws IOException {
        Path packRoot = this.temporaryDirectory.resolve("pack");
        write(
                packRoot.resolve(
                        "assets/example/models/block/test.json"
                ),
                "model"
        );
        write(
                packRoot.resolve("assets/example/root.txt"),
                "root"
        );
        write(
                packRoot.resolve(
                        "data/example/tags/items/test.json"
                ),
                "tag"
        );
        Files.createDirectories(
                packRoot.resolve("assets/empty_namespace")
        );
        Files.createDirectories(
                packRoot.resolve("assets/UPPER")
        );

        PackResourcesIndex index =
                PathPackResourcesIndex.build(packRoot);
        assertNotNull(index);
        assertTrue(index.getNamespaces(PackType.CLIENT_RESOURCES)
                .containsAll(List.of("example", "empty_namespace")));
        assertTrue(index.getNamespaces(PackType.CLIENT_RESOURCES)
                .stream()
                .noneMatch("UPPER"::equals));
        assertEquals(
                "model",
                read(index.getResource(
                        PackType.CLIENT_RESOURCES,
                        ResourceLocation.fromNamespaceAndPath(
                                "example",
                                "models/block/test.json"
                        )
                ))
        );

        List<ResourceLocation> models = new ArrayList<>();
        index.listResources(
                PackType.CLIENT_RESOURCES,
                "example",
                "models",
                (location, supplier) -> models.add(location)
        );
        assertEquals(
                List.of(ResourceLocation.fromNamespaceAndPath(
                        "example",
                        "models/block/test.json"
                )),
                models
        );
        assertNull(index.getResource(
                PackType.CLIENT_RESOURCES,
                ResourceLocation.fromNamespaceAndPath(
                        "example",
                        "models/missing.json"
                )
        ));

        List<ResourceLocation> allResources = new ArrayList<>();
        index.listResources(
                PackType.CLIENT_RESOURCES,
                "example",
                "",
                (location, supplier) -> allResources.add(location)
        );
        assertEquals(2, allResources.size());
    }

    @Test
    void preservesForgeNamespaceDiscoveryRules() throws IOException {
        Path assets =
                this.temporaryDirectory.resolve("forge/assets");
        Files.createDirectories(assets.resolve("UPPER"));
        write(assets.resolve("direct_file"), "not-a-namespace");

        Map<PackType, List<Path>> pathsForType =
                new EnumMap<>(PackType.class);
        pathsForType.put(
                PackType.CLIENT_RESOURCES,
                List.of(assets)
        );

        PackResourcesIndex index =
                PathPackResourcesIndex.buildForge(pathsForType);
        assertNotNull(index);
        assertEquals(
                java.util.Set.of("UPPER"),
                index.getNamespaces(PackType.CLIENT_RESOURCES)
        );
    }

    @Test
    void preservesVanillaRootPriority() throws IOException {
        Path highPriority =
                this.temporaryDirectory.resolve("high/assets");
        Path lowPriority =
                this.temporaryDirectory.resolve("low/assets");
        write(
                highPriority.resolve("example/models/shared.json"),
                "high"
        );
        write(
                lowPriority.resolve("example/models/shared.json"),
                "low"
        );
        write(
                lowPriority.resolve("example/models/low_only.json"),
                "low-only"
        );

        Map<PackType, List<Path>> pathsForType =
                new EnumMap<>(PackType.class);
        pathsForType.put(
                PackType.CLIENT_RESOURCES,
                List.of(highPriority, lowPriority)
        );

        PackResourcesIndex index =
                PathPackResourcesIndex.build(pathsForType);
        assertNotNull(index);
        assertEquals(
                "high",
                read(index.getResource(
                        PackType.CLIENT_RESOURCES,
                        ResourceLocation.fromNamespaceAndPath(
                                "example",
                                "models/shared.json"
                        )
                ))
        );

        Map<ResourceLocation, String> listed = new HashMap<>();
        index.listResources(
                PackType.CLIENT_RESOURCES,
                "example",
                "models",
                (location, supplier) -> {
                    try {
                        listed.put(location, read(supplier));
                    } catch (IOException exception) {
                        throw new AssertionError(exception);
                    }
                }
        );
        assertEquals(2, listed.size());
        assertEquals(
                "high",
                listed.get(ResourceLocation.fromNamespaceAndPath(
                        "example",
                        "models/shared.json"
                ))
        );
        assertEquals(
                "low-only",
                listed.get(ResourceLocation.fromNamespaceAndPath(
                        "example",
                        "models/low_only.json"
                ))
        );
    }

    private static void write(Path path, String contents)
            throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static String read(IoSupplier<InputStream> supplier)
            throws IOException {
        assertNotNull(supplier);
        try (InputStream stream = supplier.get()) {
            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
