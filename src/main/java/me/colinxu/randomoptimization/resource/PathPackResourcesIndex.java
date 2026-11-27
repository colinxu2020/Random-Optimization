package me.colinxu.randomoptimization.resource;

import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
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
        } catch (IOException | IllegalArgumentException exception) {
            // Let the pack handle providers whose resource roots cannot be walked.
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
            for (var entry : pathsForType.entrySet()) {
                for (Path typeRoot : entry.getValue()) {
                    // Vanilla stores roots from highest to lowest priority.
                    walkTypeRoot(
                            builder,
                            entry.getKey(),
                            typeRoot,
                            lowercaseNamespacesOnly,
                            filesCanDeclareNamespaces
                    );
                }
            }
            return builder.build();
        } catch (IOException | IllegalArgumentException exception) {
            // Let the pack handle providers whose resource roots cannot be walked.
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
        record Directory(String namespace, Path namespaceRoot, String prefix) {}
        var directories = new ArrayDeque<Directory>();
        Files.walkFileTree(typeRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                Directory parent = directories.peek();
                if (parent == null) {
                    directories.push(new Directory(null, null, ""));
                } else if (parent.namespace() == null) {
                    String namespace = dir.getFileName().toString();
                    addNamespace(builder, packType, namespace, lowercaseNamespacesOnly);
                    directories.push(new Directory(namespace, dir, ""));
                } else {
                    String prefix = parent.prefix() + dir.getFileName() + "/";
                    directories.push(new Directory(parent.namespace(), parent.namespaceRoot(), prefix));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                directories.pop();
                return super.postVisitDirectory(dir, error);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                Directory parent = directories.peek();
                if (parent == null) {
                    return FileVisitResult.CONTINUE;
                }
                if (parent.namespace() == null) {
                    if (filesCanDeclareNamespaces) {
                        addNamespace(builder, packType, file.getFileName().toString(), lowercaseNamespacesOnly);
                    }
                } else if (attributes.isRegularFile()) {
                    String resourcePath = parent.prefix() + file.getFileName();
                    Path namespaceRoot = parent.namespaceRoot();
                    builder.addResource(packType, parent.namespace(), resourcePath,
                            () -> Files.newInputStream(namespaceRoot.resolve(resourcePath)));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException error) throws IOException {
                if (file.equals(typeRoot)
                        && (error instanceof NoSuchFileException || error instanceof NotDirectoryException)) {
                    return FileVisitResult.CONTINUE;
                }
                return super.visitFileFailed(file, error);
            }
        });
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
}
