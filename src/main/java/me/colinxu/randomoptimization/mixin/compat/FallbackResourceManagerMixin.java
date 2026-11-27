package me.colinxu.randomoptimization.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.colinxu.randomoptimization.resource.NamespaceFilteredPack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.List;

@Mixin(FallbackResourceManager.class)
public abstract class FallbackResourceManagerMixin {
    @Shadow @Final private PackType type;

    @WrapOperation(method = "getResourceStack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/PackResources;getChildren()Ljava/util/Collection;", remap = false),
            require = 0)
    private Collection<PackResources> randomoptimization$stackChildren(PackResources pack,
            Operation<Collection<PackResources>> original, ResourceLocation location) {
        Collection<PackResources> children = original.call(pack);
        if (children != null && type == PackType.CLIENT_RESOURCES && pack instanceof NamespaceFilteredPack filtered) {
            List<PackResources> candidates = filtered.randomoptimization$candidates(type, location.getNamespace(), children);
            if (candidates != null) {
                return candidates;
            }
        }
        return children;
    }
}
