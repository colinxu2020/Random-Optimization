package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.resource.FilePackResourcesIndex;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.zip.ZipFile;

@Mixin(value = FilePackResources.class, priority = 900)
public abstract class FilePackResourcesMixin {
    @Shadow
    private ZipFile zipFile;

    @Shadow
    protected abstract ZipFile getOrCreateZipFile();

    @Unique
    private final Object randomoptimization$indexLock = new Object();

    @Unique
    private volatile FilePackResourcesIndex randomoptimization$index;

    @Unique
    private boolean randomoptimization$closing;

    @Inject(method = "getOrCreateZipFile", at = @At("RETURN"))
    private void randomoptimization$initializeIndex(CallbackInfoReturnable<ZipFile> cir) {
        ZipFile openedZip = cir.getReturnValue();
        if (openedZip == null || this.randomoptimization$index != null) {
            return;
        }

        synchronized (this.randomoptimization$indexLock) {
            if (this.randomoptimization$index == null
                    && !this.randomoptimization$closing
                    && this.zipFile == openedZip) {
                this.randomoptimization$index = FilePackResourcesIndex.build(openedZip);
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void randomoptimization$discardIndex(CallbackInfo ci) {
        synchronized (this.randomoptimization$indexLock) {
            this.randomoptimization$closing = true;
            this.randomoptimization$index = null;
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void randomoptimization$finishDiscardingIndex(CallbackInfo ci) {
        synchronized (this.randomoptimization$indexLock) {
            this.randomoptimization$index = null;
            this.randomoptimization$closing = false;
        }
    }

    /**
     * @author RandomOptimization
     * @reason Serve recursive directory listings from a precomputed index.
     */
    @Overwrite
    public void listResources(@NotNull PackType packType, @NotNull String namespace,
                              @NotNull String path,
                              PackResources.@NotNull ResourceOutput resourceOutput) {
        if (this.getOrCreateZipFile() == null) {
            return;
        }

        FilePackResourcesIndex index = this.randomoptimization$index;
        if (index != null) {
            index.listResources(packType, namespace, path, resourceOutput);
        }
    }

    /**
     * @author RandomOptimization
     * @reason Return the namespace set collected while the ZIP index was built.
     */
    @Overwrite
    public @NotNull Set<String> getNamespaces(@NotNull PackType packType) {
        if (this.getOrCreateZipFile() == null) {
            return Set.of();
        }

        FilePackResourcesIndex index = this.randomoptimization$index;
        return index == null ? Set.of() : index.getNamespaces(packType);
    }
}
