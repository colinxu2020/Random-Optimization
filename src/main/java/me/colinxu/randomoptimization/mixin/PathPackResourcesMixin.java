package me.colinxu.randomoptimization.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.colinxu.randomoptimization.resource.PackResourcesIndex;
import me.colinxu.randomoptimization.resource.PackTypeCache;
import me.colinxu.randomoptimization.resource.PathPackResourcesIndex;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(PathPackResources.class)
public abstract class PathPackResourcesMixin {
    @Shadow
    @Final
    private Path root;

    @Unique
    private final PackTypeCache<PackResourcesIndex> randomoptimization$indexes = new PackTypeCache<>();

    @Unique
    private final PackTypeCache<Set<String>> randomoptimization$namespaces = new PackTypeCache<>();

    @Unique
    private PackResourcesIndex randomoptimization$getIndex(PackType packType) {
        return randomoptimization$indexes.get(packType, () -> {
            Path typeRoot = root.resolve(packType.getDirectory());
            return PathPackResourcesIndex.build(Map.of(packType, List.of(typeRoot)));
        });
    }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$getResource(
            PackType packType,
            ResourceLocation location,
            CallbackInfoReturnable<IoSupplier<InputStream>> cir
    ) {
        PackResourcesIndex index = this.randomoptimization$getIndex(packType);
        if (index != null) {
            IoSupplier<InputStream> resource = index.getResource(packType, location);
            if (resource != null) {
                cir.setReturnValue(resource);
            }
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

        PackResourcesIndex index = this.randomoptimization$getIndex(packType);
        if (index != null) {
            index.listResources(packType, namespace, path, output);
            ci.cancel();
        }
    }

    @WrapMethod(method = "getNamespaces")
    private Set<String> randomoptimization$getNamespaces(PackType type, Operation<Set<String>> original) {
        return randomoptimization$namespaces.get(type, () -> Set.copyOf(original.call(type)));
    }
}
