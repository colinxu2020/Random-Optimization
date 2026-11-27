package me.colinxu.randomoptimization.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.colinxu.randomoptimization.resource.NamespaceFilteredPack;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(value = DelegatingPackResources.class, remap = false)
public abstract class DelegatingPackResourcesMixin implements NamespaceFilteredPack {
    @Shadow @Final private List<PackResources> delegates;
    @Shadow @Final private Map<String, List<PackResources>> namespacesAssets;
    @Shadow @Final private Map<String, List<PackResources>> namespacesData;

    @Unique private boolean randomoptimization$filterAssets;
    @Unique private boolean randomoptimization$filterData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void randomoptimization$checkChildren(CallbackInfo ci) {
        for (PackResources child : delegates) {
            Class<?> implementation = child.getClass();
            if (implementation != PathPackResources.class && implementation.getEnclosingClass() != ResourcePackLoader.class) {
                return;
            }
        }
        // A case-insensitive provider may resolve a lowercase request against a mixed-case namespace.
        randomoptimization$filterAssets = randomoptimization$hasLowercaseNamespaces(namespacesAssets);
        randomoptimization$filterData = randomoptimization$hasLowercaseNamespaces(namespacesData);
    }

    @Unique
    private static boolean randomoptimization$hasLowercaseNamespaces(Map<String, List<PackResources>> namespaces) {
        return namespaces.keySet().stream().allMatch(name -> name.equals(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    @Nullable
    public List<PackResources> randomoptimization$candidates(PackType type, String namespace,
                                                              Collection<PackResources> children) {
        if (children != delegates) {
            return null;
        }
        if (type == PackType.CLIENT_RESOURCES) {
            return randomoptimization$filterAssets ? namespacesAssets.getOrDefault(namespace, List.of()) : null;
        }
        return randomoptimization$filterData ? namespacesData.getOrDefault(namespace, List.of()) : null;
    }

    @ModifyExpressionValue(method = {"listResources", "m_8031_"}, at = @At(value = "FIELD",
            target = "Lnet/minecraftforge/resource/DelegatingPackResources;delegates:Ljava/util/List;"))
    private List<PackResources> randomoptimization$listChildren(List<PackResources> children, PackType type,
                                                                 String namespace, String path, PackResources.ResourceOutput output) {
        List<PackResources> candidates = randomoptimization$candidates(type, namespace, children);
        return candidates == null ? children : candidates;
    }
}
