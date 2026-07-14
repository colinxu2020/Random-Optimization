package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.resource.FilePackResourcesIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePackResourcesIndexTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void helperClassIsOutsideTheDefinedMixinPackage() {
        String packageName = FilePackResourcesIndex.class.getPackageName();
        assertFalse(packageName.equals("me.colinxu.randomoptimization.mixin")
                || packageName.startsWith("me.colinxu.randomoptimization.mixin."));
    }

    @Test
    void indexesRecursiveListingsAndNamespacesInArchiveOrder() throws IOException {
        Path archive = this.temporaryDirectory.resolve("pack.zip");
        try (ZipOutputStream output = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
            add(output, "assets/minecraft/models/block/stone.json", "block");
            add(output, "assets/minecraft/models/item/stone.json", "item");
            add(output, "assets/minecraft/root.txt", "root");
            add(output, "assets/minecraft//odd.json", "odd");
            add(output, "assets/mod/lang/en_us.json", "language");
            add(output, "assets/mod/models/Invalid.json", "invalid");
            add(output, "assets//ghost/file.json", "ghost");
            addDirectory(output, "assets/directory_only/");
            add(output, "assets/UPPER/lang/en_us.json", "upper");
            add(output, "data/example/tags/items/test.json", "tag");
            add(output, "pack.mcmeta", "metadata");
        }

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            FilePackResourcesIndex index = FilePackResourcesIndex.build(zipFile);

            assertEquals(Set.of("minecraft", "mod", "ghost", "directory_only"),
                    index.getNamespaces(PackType.CLIENT_RESOURCES));
            assertEquals(Set.of("example"), index.getNamespaces(PackType.SERVER_DATA));

            List<Result> models = list(index, PackType.CLIENT_RESOURCES, "minecraft", "models");
            assertEquals(List.of(
                    ResourceLocation.fromNamespaceAndPath(
                            "minecraft", "models/block/stone.json"),
                    ResourceLocation.fromNamespaceAndPath(
                            "minecraft", "models/item/stone.json")
            ), models.stream().map(Result::location).toList());
            assertEquals(List.of("block", "item"), readAll(models));

            List<Result> blocks =
                    list(index, PackType.CLIENT_RESOURCES, "minecraft", "models/block");
            assertEquals(List.of("block"), readAll(blocks));

            // Vanilla appends '/' even to an empty requested path. Consequently only a
            // physical double slash after the namespace matches an empty-path listing.
            List<Result> emptyPath =
                    list(index, PackType.CLIENT_RESOURCES, "minecraft", "");
            assertEquals(List.of(ResourceLocation.fromNamespaceAndPath(
                            "minecraft", "/odd.json")),
                    emptyPath.stream().map(Result::location).toList());
            assertEquals(List.of("odd"), readAll(emptyPath));

            // Invalid paths and entries from another namespace/type never leak into a bucket.
            assertTrue(list(index, PackType.CLIENT_RESOURCES, "mod", "models").isEmpty());
            assertTrue(list(index, PackType.CLIENT_RESOURCES, "example", "tags").isEmpty());
            assertEquals(List.of("tag"), readAll(
                    list(index, PackType.SERVER_DATA, "example", "tags")));
        }
    }

    @Test
    void supportsFusionOverrideOutputWrapping() throws IOException {
        Path archive = this.temporaryDirectory.resolve("fusion-pack.zip");
        try (ZipOutputStream output = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
            add(output, "assets/minecraft/models/block/stone.json", "base stone");
            add(output, "assets/minecraft/models/block/dirt.json", "base dirt");
            add(output,
                    "fusion_overrides/assets/minecraft/models/block/stone.json",
                    "override stone");
        }

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            FilePackResourcesIndex index = FilePackResourcesIndex.build(zipFile);
            List<Result> results = listWithFusionOverrides(
                    index, zipFile, PackType.CLIENT_RESOURCES,
                    "minecraft", "models", "fusion_overrides/");

            assertEquals(List.of(
                    ResourceLocation.fromNamespaceAndPath(
                            "minecraft", "models/block/stone.json"),
                    ResourceLocation.fromNamespaceAndPath(
                            "minecraft", "models/block/dirt.json")
            ), results.stream().map(Result::location).toList());
            assertEquals(List.of("override stone", "base dirt"), readAll(results));
        }
    }

    private static List<Result> list(FilePackResourcesIndex index, PackType type,
                                     String namespace, String path) {
        List<Result> results = new ArrayList<>();
        PackResources.ResourceOutput output =
                (location, supplier) -> results.add(new Result(location, supplier));
        index.listResources(type, namespace, path, output);
        return results;
    }

    /** Mirrors Fusion's HEAD ResourceOutput wrapper around FilePackResources.listResources. */
    private static List<Result> listWithFusionOverrides(
            FilePackResourcesIndex index, ZipFile zipFile, PackType type,
            String namespace, String path, String overridesFolder) {
        List<Result> results = new ArrayList<>();
        Set<ResourceLocation> overriddenLocations = new HashSet<>();
        String namespaceDirectory =
                overridesFolder + type.getDirectory() + '/' + namespace + '/';
        String pathDirectory = namespaceDirectory + path + '/';

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith(pathDirectory)) {
                continue;
            }

            ResourceLocation location = ResourceLocation.tryBuild(
                    namespace, name.substring(namespaceDirectory.length()));
            if (location != null) {
                overriddenLocations.add(location);
                results.add(new Result(location, IoSupplier.create(zipFile, entry)));
            }
        }

        index.listResources(type, namespace, path, (location, supplier) -> {
            if (!overriddenLocations.contains(location)) {
                results.add(new Result(location, supplier));
            }
        });
        return results;
    }

    private static List<String> readAll(List<Result> results) throws IOException {
        List<String> contents = new ArrayList<>(results.size());
        for (Result result : results) {
            try (InputStream stream = result.supplier.get()) {
                contents.add(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return contents;
    }

    private static void add(ZipOutputStream output, String name, String contents)
            throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(contents.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static void addDirectory(ZipOutputStream output, String name) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.closeEntry();
    }

    private record Result(ResourceLocation location, IoSupplier<InputStream> supplier) {
    }
}
