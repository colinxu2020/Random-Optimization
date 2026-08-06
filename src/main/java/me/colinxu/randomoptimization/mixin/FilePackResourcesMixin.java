package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.FilePackResourcesIndex;
import me.colinxu.randomoptimization.SharedZipFileAccessIndex;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.loading.LoadingModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin {
    @Shadow
    @Final
    private FilePackResources.SharedZipFileAccess zipFileAccess;

    @Shadow
    @Final
    private String prefix;

    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$getNamespaces(
            PackType packType,
            CallbackInfoReturnable<Set<String>> cir
    ) {
        FilePackResourcesIndex index =
                ((SharedZipFileAccessIndex) this.zipFileAccess).randomoptimization$getIndex();
        cir.setReturnValue(index == null
                ? Set.of()
                : index.getNamespaces(this.prefix, packType));
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$listResources(
            PackType packType,
            String namespace,
            String path,
            PackResources.ResourceOutput output,
            CallbackInfo ci
    ) {
        FilePackResourcesIndex index =
                ((SharedZipFileAccessIndex) this.zipFileAccess).randomoptimization$getIndex();
        if (index != null) {
            index.listResources(
                    this.prefix,
                    packType,
                    namespace,
                    path,
                    path.isEmpty()
                            && LoadingModList.get().getModFileById("fancymenu") != null,
                    output
            );
        }
        ci.cancel();
    }
}
