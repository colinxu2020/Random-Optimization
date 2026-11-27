package me.colinxu.randomoptimization.mixin.compat;

import me.colinxu.randomoptimization.resource.PackResourcesIndex;
import me.colinxu.randomoptimization.resource.PackTypeCache;
import me.colinxu.randomoptimization.resource.PathPackResourcesIndex;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.resource.PathPackResources;
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

@Mixin(value = PathPackResources.class, remap = false)
public abstract class ForgePathPackResourcesMixin {
    /*
     * PathPackResources is a Forge class and therefore must not be remapped,
     * but these three methods override Minecraft APIs. ForgeGradle exposes
     * their mapped names in development while production Forge keeps their
     * SRG names. Keep both selectors so the injections work in either
     * environment.
     */
    @Shadow
    protected abstract Path resolve(String... paths);

    @Shadow
    private Set<String> getNamespacesFromDisk(PackType type) {
        throw new AssertionError();
    }

    @Unique
    private final PackTypeCache<PackResourcesIndex> randomoptimization$indexes = new PackTypeCache<>();

    @Unique
    private final PackTypeCache<Set<String>> randomoptimization$namespaces = new PackTypeCache<>();

    @Unique
    private PackResourcesIndex randomoptimization$getIndex(PackType packType) {
        return randomoptimization$indexes.get(packType, () -> {
            Path typeRoot = resolve(packType.getDirectory());
            return PathPackResourcesIndex.buildForge(Map.of(packType, List.of(typeRoot)));
        });
    }

    @Inject(
            method = {"getResource", "m_214146_"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void randomoptimization$getResource(
            PackType packType,
            ResourceLocation location,
            CallbackInfoReturnable<IoSupplier<InputStream>> cir
    ) {
        PackType effectiveType = location.getPath().startsWith("lang/") ? PackType.CLIENT_RESOURCES : packType;
        PackResourcesIndex index = this.randomoptimization$getIndex(effectiveType);
        if (index != null) {
            IoSupplier<InputStream> resource = index.getResource(effectiveType, location);
            if (resource != null) {
                cir.setReturnValue(resource);
            }
        }
    }

    @Inject(
            method = {"listResources", "m_8031_"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
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

    @Inject(
            method = {"getNamespaces", "m_5698_"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void randomoptimization$getNamespaces(
            PackType packType,
            CallbackInfoReturnable<Set<String>> cir
    ) {
        cir.setReturnValue(randomoptimization$namespaces.get(packType,
                () -> Set.copyOf(getNamespacesFromDisk(packType))));
    }
}
