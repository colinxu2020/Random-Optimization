package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.PackResourcesIndex;
import me.colinxu.randomoptimization.PathPackResourcesIndex;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

@Mixin(PathPackResources.class)
public abstract class PathPackResourcesMixin {
    @Shadow
    @Final
    private Path root;

    @Unique
    private volatile boolean randomoptimization$indexInitialized;

    @Unique
    private PackResourcesIndex randomoptimization$index;

    @Unique
    private PackResourcesIndex randomoptimization$getIndex() {
        if (!this.randomoptimization$indexInitialized) {
            synchronized (this) {
                if (!this.randomoptimization$indexInitialized) {
                    this.randomoptimization$index =
                            PathPackResourcesIndex.build(this.root);
                    this.randomoptimization$indexInitialized = true;
                }
            }
        }
        return this.randomoptimization$index;
    }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$getResource(
            PackType packType,
            ResourceLocation location,
            CallbackInfoReturnable<IoSupplier<InputStream>> cir
    ) {
        PackResourcesIndex index = this.randomoptimization$getIndex();
        if (index != null) {
            cir.setReturnValue(index.getResource(packType, location));
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$listResources(
            PackType packType,
            String namespace,
            String path,
            PackResources.ResourceOutput output,
            CallbackInfo ci
    ) {
        if (FileUtil.decomposePath(path).result().isEmpty()) {
            return;
        }

        PackResourcesIndex index = this.randomoptimization$getIndex();
        if (index != null) {
            index.listResources(packType, namespace, path, output);
            ci.cancel();
        }
    }

    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$getNamespaces(
            PackType packType,
            CallbackInfoReturnable<Set<String>> cir
    ) {
        PackResourcesIndex index = this.randomoptimization$getIndex();
        if (index != null) {
            cir.setReturnValue(index.getNamespaces(packType));
        }
    }
}
