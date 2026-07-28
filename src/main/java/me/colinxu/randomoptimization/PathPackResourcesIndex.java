package me.colinxu.randomoptimization;

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
 * Builds {@link PackResourcesIndex} instances with one NIO tree walk per root.
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
                        packRoot.resolve(packType.getDirectory())
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
        PackResourcesIndex.Builder builder = PackResourcesIndex.builder();
        try {
            for (PackType packType : PackType.values()) {
                for (Path typeRoot : pathsForType.getOrDefault(
                        packType,
                        List.of()
                )) {
                    // Roots are already ordered from highest to lowest priority.
                    walkTypeRoot(builder, packType, typeRoot);
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
            Path typeRoot
    ) throws IOException {
        try {
            Files.walkFileTree(typeRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(
                        Path directory,
                        BasicFileAttributes attributes
                ) {
                    Path relative = typeRoot.relativize(directory);
                    if (relative.getNameCount() == 1) {
                        builder.addNamespace(
                                packType,
                                relative.getName(0).toString()
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
                        builder.addNamespace(
                                packType,
                                relative.getName(0).toString()
                        );
                    } else if (nameCount > 1 && attributes.isRegularFile()) {
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
