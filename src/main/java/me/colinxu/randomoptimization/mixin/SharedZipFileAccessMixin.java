package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.FilePackResourcesIndex;
import me.colinxu.randomoptimization.SharedZipFileAccessIndex;
import net.minecraft.server.packs.FilePackResources;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.zip.ZipFile;

@Mixin(FilePackResources.SharedZipFileAccess.class)
public abstract class SharedZipFileAccessMixin implements SharedZipFileAccessIndex {
    @Shadow
    @Nullable
    abstract ZipFile getOrCreateZipFile();

    @Unique
    private volatile ZipFile randomoptimization$indexedZipFile;

    @Unique
    private volatile FilePackResourcesIndex randomoptimization$cachedIndex;

    @Override
    public synchronized @Nullable FilePackResourcesIndex randomoptimization$getIndex() {
        ZipFile zipFile = this.getOrCreateZipFile();
        if (zipFile == null) {
            this.randomoptimization$indexedZipFile = null;
            this.randomoptimization$cachedIndex = null;
            return null;
        }

        if (this.randomoptimization$cachedIndex == null
                || this.randomoptimization$indexedZipFile != zipFile) {
            this.randomoptimization$cachedIndex = FilePackResourcesIndex.build(zipFile);
            this.randomoptimization$indexedZipFile = zipFile;
        }
        return this.randomoptimization$cachedIndex;
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void randomoptimization$clearIndex(CallbackInfo ci) {
        this.randomoptimization$indexedZipFile = null;
        this.randomoptimization$cachedIndex = null;
    }
}
