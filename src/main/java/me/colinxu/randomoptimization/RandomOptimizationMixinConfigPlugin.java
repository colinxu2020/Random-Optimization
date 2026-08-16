package me.colinxu.randomoptimization;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class RandomOptimizationMixinConfigPlugin implements IMixinConfigPlugin {
    static final String FILE_PACK_RESOURCES_MIXIN =
            "me.colinxu.randomoptimization.mixin.FilePackResourcesMixin";
    static final String PATH_PACK_RESOURCES_MIXIN =
            "me.colinxu.randomoptimization.mixin.PathPackResourcesMixin";
    static final String VANILLA_PACK_RESOURCES_MIXIN =
            "me.colinxu.randomoptimization.mixin.VanillaPackResourcesMixin";
    static final String FORGE_PATH_PACK_RESOURCES_MIXIN =
            "me.colinxu.randomoptimization.mixin.compat.ForgePathPackResourcesMixin";
    static final String MODERN_FIX_FILE_PACK_RESOURCES_MIXIN =
            "me.colinxu.randomoptimization.mixin.compat.ModernFixFilePackResourcesMixin";

    private boolean optimizePackLookup = true;
    private boolean lazyDFU = true;
    private boolean modernFixLoaded;

    @Override
    public void onLoad(String mixinPackage){
        StartupConfig config = StartupConfig.load();
        this.lazyDFU = config.lazyDfu();
        this.optimizePackLookup = config.optimizePackLookup();
        this.modernFixLoaded = LoadingModList.get().getModFileById("modernfix") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        if(mixinClassName.equals(FILE_PACK_RESOURCES_MIXIN)
                || mixinClassName.equals(PATH_PACK_RESOURCES_MIXIN)
                || mixinClassName.equals(VANILLA_PACK_RESOURCES_MIXIN)
                || mixinClassName.equals(FORGE_PATH_PACK_RESOURCES_MIXIN)) {
            return this.optimizePackLookup;
        }
        if(mixinClassName.equals(MODERN_FIX_FILE_PACK_RESOURCES_MIXIN)) {
            return shouldDisableModernFixPackOptimization(
                    this.optimizePackLookup, this.modernFixLoaded);
        }
        if(mixinClassName.equals("me.colinxu.randomoptimization.mixin.SharedConstantsMixin")){
            return this.lazyDFU;
        }
        return true;
    }

    static boolean shouldDisableModernFixPackOptimization(
            boolean optimizePackLookup, boolean modernFixLoaded) {
        // Never suppress ModernFix when our own replacement is disabled.
        return optimizePackLookup && modernFixLoaded;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
