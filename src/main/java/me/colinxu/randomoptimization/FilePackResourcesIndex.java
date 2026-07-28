package me.colinxu.randomoptimization;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Prefix-aware adapter that builds the shared pack index from ZIP entries.
 */
public final class FilePackResourcesIndex {
    private final ZipFile zipFile;
    private final ZipEntry[] entries;
    private final Map<String, PackResourcesIndex> indexesByPrefix = new HashMap<>();

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
        return new FilePackResourcesIndex(
                zipFile,
                entries.toArray(ZipEntry[]::new)
        );
    }

    public Set<String> getNamespaces(String packPrefix, PackType packType) {
        return this.getOrCreateIndex(packPrefix).getNamespaces(packType);
    }

    public void listResources(
            String packPrefix,
            PackType packType,
            String namespace,
            String path,
            PackResources.ResourceOutput output
    ) {
        this.getOrCreateIndex(packPrefix)
                .listResources(packType, namespace, path, output);
    }

    private synchronized PackResourcesIndex getOrCreateIndex(String packPrefix) {
        return this.indexesByPrefix.computeIfAbsent(
                packPrefix,
                this::buildIndex
        );
    }

    private PackResourcesIndex buildIndex(String packPrefix) {
        PackType[] packTypes = PackType.values();
        String[] typePrefixes = new String[packTypes.length];
        for (int i = 0; i < packTypes.length; ++i) {
            String typeDirectory = packTypes[i].getDirectory() + "/";
            typePrefixes[i] = packPrefix.isEmpty()
                    ? typeDirectory
                    : packPrefix + "/" + typeDirectory;
        }

        PackResourcesIndex.Builder builder =
                PackResourcesIndex.listingOnlyBuilder();
        for (ZipEntry entry : this.entries) {
            String entryName = entry.getName();
            for (int i = 0; i < typePrefixes.length; ++i) {
                String typePrefix = typePrefixes[i];
                if (entryName.startsWith(typePrefix)) {
                    this.accept(
                            builder,
                            packTypes[i],
                            entry,
                            entryName,
                            typePrefix.length()
                    );
                    break;
                }
            }
        }
        return builder.build();
    }

    private void accept(
            PackResourcesIndex.Builder builder,
            PackType packType,
            ZipEntry entry,
            String entryName,
            int namespaceStart
    ) {
        int namespaceEnd = entryName.indexOf('/', namespaceStart);
        String namespace = namespaceEnd < 0
                ? entryName.substring(namespaceStart)
                : entryName.substring(namespaceStart, namespaceEnd);

        // FilePackResources treats a direct assets/<namespace> entry as a
        // namespace even if it is not a directory.
        builder.addNamespace(packType, namespace);
        if (entry.isDirectory() || namespaceEnd < 0) {
            return;
        }

        String resourcePath = entryName.substring(namespaceEnd + 1);
        builder.addResource(
                packType,
                namespace,
                resourcePath,
                IoSupplier.create(this.zipFile, entry)
        );
    }
}
