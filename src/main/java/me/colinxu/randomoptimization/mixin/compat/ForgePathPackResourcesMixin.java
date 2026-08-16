package me.colinxu.randomoptimization.mixin.compat;

import me.colinxu.randomoptimization.resource.PackResourcesIndex;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
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

    @Unique
    private volatile boolean randomoptimization$indexInitialized;

    @Unique
    private PackResourcesIndex randomoptimization$index;

    @Unique
    private boolean randomoptimization$dataRootMissing;

    @Unique
    private PackResourcesIndex randomoptimization$getIndex() {
        if (!this.randomoptimization$indexInitialized) {
            synchronized (this) {
                if (!this.randomoptimization$indexInitialized) {
                    Map<PackType, List<Path>> roots =
                            new EnumMap<>(PackType.class);
                    for (PackType packType : PackType.values()) {
                        Path typeRoot = this.resolve(
                                packType.getDirectory()
                        );
                        roots.put(packType, List.of(typeRoot));
                        if (packType == PackType.SERVER_DATA) {
                            this.randomoptimization$dataRootMissing =
                                    !Files.isDirectory(typeRoot);
                        }
                    }
                    this.randomoptimization$index =
                            PathPackResourcesIndex.buildForge(roots);
                    this.randomoptimization$indexInitialized = true;
                }
            }
        }
        return this.randomoptimization$index;
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
        PackResourcesIndex index = this.randomoptimization$getIndex();
        if (index != null) {
            PackType effectiveType =
                    location.getPath().startsWith("lang/")
                            ? PackType.CLIENT_RESOURCES
                            : packType;
            cir.setReturnValue(
                    index.getResource(effectiveType, location)
            );
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

        PackResourcesIndex index = this.randomoptimization$getIndex();
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
        PackResourcesIndex index = this.randomoptimization$getIndex();
        if (index != null) {
            PackType effectiveType =
                    packType == PackType.SERVER_DATA
                            && this.randomoptimization$dataRootMissing
                            ? PackType.CLIENT_RESOURCES
                            : packType;
            cir.setReturnValue(index.getNamespaces(effectiveType));
        }
    }
}
