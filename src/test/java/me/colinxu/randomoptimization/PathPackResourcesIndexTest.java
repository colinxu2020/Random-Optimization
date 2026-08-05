package me.colinxu.randomoptimization;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathPackResourcesIndexTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void indexesNamespacesRecursiveListingsAndIndividualResources()
            throws IOException {
        Path packRoot = this.temporaryDirectory.resolve("pack");
        write(
                packRoot.resolve("assets/example/models/block/test.json"),
                "model"
        );
        write(packRoot.resolve("assets/example/root.txt"), "root");
        write(
                packRoot.resolve("data/example/tags/items/test.json"),
                "tag"
        );
        Files.createDirectories(packRoot.resolve("assets/empty_namespace"));

        PackResourcesIndex index = PathPackResourcesIndex.build(packRoot);
        assertNotNull(index);
        assertTrue(index.getNamespaces(PackType.CLIENT_RESOURCES)
                .containsAll(List.of("example", "empty_namespace")));
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
    void preservesVanillaRootPriorityWhileKeepingLowerPriorityResources()
            throws IOException {
        Path highPriority = this.temporaryDirectory.resolve("high/assets");
        Path lowPriority = this.temporaryDirectory.resolve("low/assets");
        write(highPriority.resolve("example/models/shared.json"), "high");
        write(lowPriority.resolve("example/models/shared.json"), "low");
        write(lowPriority.resolve("example/models/low_only.json"), "low-only");

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
        assertEquals(
                "low-only",
                read(index.getResource(
                        PackType.CLIENT_RESOURCES,
                        ResourceLocation.fromNamespaceAndPath(
                                "example",
                                "models/low_only.json"
                        )
                ))
        );

        Map<ResourceLocation, String> listed = new java.util.HashMap<>();
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

    @Test
    void indexesMinecraftLinkFileSystemRoots() throws IOException {
        Path shader = this.temporaryDirectory.resolve("blit_screen.json");
        write(shader, "shader");

        try (FileSystem fileSystem = LinkFileSystem.builder()
                .put(
                        List.of(
                                "assets",
                                "minecraft",
                                "shaders",
                                "core",
                                "blit_screen.json"
                        ),
                        shader
                )
                .build("test-vanilla-resources")) {
            Map<PackType, List<Path>> pathsForType =
                    new EnumMap<>(PackType.class);
            pathsForType.put(
                    PackType.CLIENT_RESOURCES,
                    List.of(fileSystem.getPath("/assets"))
            );

            PackResourcesIndex index =
                    PathPackResourcesIndex.build(pathsForType);

            assertNotNull(index);
            assertEquals(
                    "shader",
                    read(index.getResource(
                            PackType.CLIENT_RESOURCES,
                            ResourceLocation.fromNamespaceAndPath(
                                    "minecraft",
                                    "shaders/core/blit_screen.json"
                            )
                    ))
            );
        }
    }

    @Test
    void sharedIndexKeepsZipPrefixAndRecursiveListingBehavior()
            throws IOException {
        Path zipPath = this.temporaryDirectory.resolve("pack.zip");
        try (ZipOutputStream output =
                     new ZipOutputStream(Files.newOutputStream(zipPath))) {
            putEntry(
                    output,
                    "overlay/assets/example/models/block/test.json",
                    "zip-model"
            );
            putEntry(
                    output,
                    "assets/base/models/item/base.json",
                    "base-model"
            );
        }

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            FilePackResourcesIndex index =
                    FilePackResourcesIndex.build(zipFile);
            assertEquals(
                    java.util.Set.of("example"),
                    index.getNamespaces(
                            "overlay",
                            PackType.CLIENT_RESOURCES
                    )
            );

            List<String> listed = new ArrayList<>();
            index.listResources(
                    "overlay",
                    PackType.CLIENT_RESOURCES,
                    "example",
                    "models",
                    (location, supplier) -> {
                        try {
                            listed.add(location + "=" + read(supplier));
                        } catch (IOException exception) {
                            throw new AssertionError(exception);
                        }
                    }
            );
            assertEquals(
                    List.of(
                            "example:models/block/test.json=zip-model"
                    ),
                    listed
            );
        }
    }

    private static void write(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static String read(IoSupplier<InputStream> supplier)
            throws IOException {
        assertNotNull(supplier);
        try (InputStream stream = supplier.get()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void putEntry(
            ZipOutputStream output,
            String name,
            String contents
    ) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(contents.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
