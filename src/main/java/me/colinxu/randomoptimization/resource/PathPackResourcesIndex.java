package me.colinxu.randomoptimization.resource;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

/**
 * Builds a pack index with one NIO tree walk per assets/data root.
 */
public final class PathPackResourcesIndex {
    private PathPackResourcesIndex() {
    }

    @Nullable
    public static PackResourcesIndex build(Path packRoot) {
        PackResourcesIndex.Builder builder = PackResourcesIndex.builder();
        try {
            for (PackType packType : PackType.values()) {
                walkTypeRoot(
                        builder,
                        packType,
                        packRoot.resolve(packType.getDirectory()),
                        true,
                        true
                );
            }
            return builder.build();
        } catch (IOException exception) {
            return null;
        }
    }

    @Nullable
    public static PackResourcesIndex build(
            Map<PackType, List<Path>> pathsForType
    ) {
        return build(pathsForType, true, true);
    }

    /**
     * Forge's path pack accepts mixed-case directory namespaces and ignores
     * direct files under assets/data during namespace discovery.
     */
    @Nullable
    public static PackResourcesIndex buildForge(
            Map<PackType, List<Path>> pathsForType
    ) {
        return build(pathsForType, false, false);
    }

    @Nullable
    private static PackResourcesIndex build(
            Map<PackType, List<Path>> pathsForType,
            boolean lowercaseNamespacesOnly,
            boolean filesCanDeclareNamespaces
    ) {
        PackResourcesIndex.Builder builder = PackResourcesIndex.builder();
        try {
            for (PackType packType : PackType.values()) {
                for (Path typeRoot : pathsForType.getOrDefault(
                        packType,
                        List.of()
                )) {
                    // Vanilla stores roots from highest to lowest priority.
                    walkTypeRoot(
                            builder,
                            packType,
                            typeRoot,
                            lowercaseNamespacesOnly,
                            filesCanDeclareNamespaces
                    );
                }
            }
            return builder.build();
        } catch (IOException exception) {
            return null;
        }
    }

    private static void walkTypeRoot(
            PackResourcesIndex.Builder builder,
            PackType packType,
            Path typeRoot,
            boolean lowercaseNamespacesOnly,
            boolean filesCanDeclareNamespaces
    ) throws IOException {
        try {
            Files.walkFileTree(typeRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(
                        Path directory,
                        BasicFileAttributes attributes
                ) {
                    Path relative = typeRoot.relativize(directory);
                    if (!relative.toString().isEmpty()
                            && relative.getNameCount() == 1) {
                        addNamespace(
                                builder,
                                packType,
                                relative.getName(0).toString(),
                                lowercaseNamespacesOnly
                        );
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attributes
                ) {
                    Path relative = typeRoot.relativize(file);
                    int nameCount = relative.getNameCount();
                    if (nameCount == 1) {
                        if (filesCanDeclareNamespaces) {
                            addNamespace(
                                    builder,
                                    packType,
                                    relative.getName(0).toString(),
                                    lowercaseNamespacesOnly
                            );
                        }
                    } else if (attributes.isRegularFile()) {
                        builder.addResource(
                                packType,
                                relative.getName(0).toString(),
                                resourcePath(relative),
                                IoSupplier.create(file)
                        );
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException | NotDirectoryException ignored) {
            // A missing assets or data root represents an empty pack type.
        }
    }

    private static void addNamespace(
            PackResourcesIndex.Builder builder,
            PackType packType,
            String namespace,
            boolean lowercaseOnly
    ) {
        if (lowercaseOnly) {
            builder.addNamespace(packType, namespace);
        } else {
            builder.addNamespaceUnchecked(packType, namespace);
        }
    }

    private static String resourcePath(Path relative) {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < relative.getNameCount(); ++i) {
            if (result.length() > 0) {
                result.append('/');
            }
            result.append(relative.getName(i));
        }
        return result.toString();
    }
}
