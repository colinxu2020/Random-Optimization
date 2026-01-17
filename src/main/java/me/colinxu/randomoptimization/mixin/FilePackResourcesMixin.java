package me.colinxu.randomoptimization.mixin;

import com.google.common.collect.Sets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin {
    @Shadow
    private ZipFile zipFile;

    @Shadow
    protected abstract ZipFile getOrCreateZipFile();

    @Unique
    private TreeMap<String, ZipEntry> randomoptimization$cachedEntries;

    @Inject(method="getOrCreateZipFile", at=@At("RETURN"))
    private void initIndex(CallbackInfoReturnable<ZipFile> cir) {
        if(this.randomoptimization$cachedEntries == null && this.zipFile != null){
            this.randomoptimization$cachedEntries = new TreeMap<>();
            Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                this.randomoptimization$cachedEntries.put(entry.getName(), entry);
            }
        }
    }

    @Inject(method="close", at=@At("HEAD"))
    private void cleanupIndex(CallbackInfo ci){
        if(this.randomoptimization$cachedEntries != null){
            this.randomoptimization$cachedEntries.clear();
            this.randomoptimization$cachedEntries = null;
        }
    }

    /**
     * @author RandomOptimization
     * @reason Rewrite the list resources function in order to speed up the function(configurable).
     */
    @Overwrite
    public void listResources(@NotNull PackType pPackType, @NotNull String pNamespace, @NotNull String pPath, PackResources.@NotNull ResourceOutput pResourceOutput) {
        ZipFile zipfile = this.getOrCreateZipFile();
        if (zipfile != null) {
            String s = pPackType.getDirectory() + "/" + pNamespace + "/";
            String s1 = s + pPath + "/";
            Map<String, ZipEntry> subMap = this.randomoptimization$cachedEntries.subMap(s1, s1 + Character.MAX_VALUE);

            for (Map.Entry<String, ZipEntry> entry : subMap.entrySet()) {
                ZipEntry zipentry = entry.getValue();
                if (!zipentry.isDirectory()) {
                    ResourceLocation resourcelocation = ResourceLocation.tryBuild(pNamespace, zipentry.getName().substring(s.length()));
                    if (resourcelocation != null) {
                        pResourceOutput.accept(resourcelocation, IoSupplier.create(zipfile, zipentry));
                    }
                }
            }

        }
    }

    /**
     * @author RandomOptimization
     * @reason Rewrite the get namespaces function in order to speed up the function(configurable).
     */
    @Overwrite
    public @NotNull Set<String> getNamespaces(@NotNull PackType pType) {
        ZipFile zipfile = this.getOrCreateZipFile();
        if (zipfile == null) {
            return Set.of();
        } else {
            Set<String> set = Sets.newHashSet();
            String prefix = pType.getDirectory() + "/";
            int prefixLength = prefix.length();
            String currentPath = this.randomoptimization$cachedEntries.ceilingKey(prefix);

            while(currentPath != null && currentPath.startsWith(prefix)) {
                String namespace;
                if(currentPath.indexOf('/', prefixLength) < 0){
                    namespace = currentPath.substring(prefixLength);
                }else{
                    namespace = currentPath.substring(prefixLength, currentPath.indexOf('/', prefixLength));
                }
                if(namespace.equals(namespace.toLowerCase(Locale.ROOT))){
                    set.add(namespace);
                }
                String jumpKey = prefix + namespace + "/" + Character.MAX_VALUE;
                currentPath = this.randomoptimization$cachedEntries.higherKey(jumpKey);
            }

            return set;
        }
    }
}
