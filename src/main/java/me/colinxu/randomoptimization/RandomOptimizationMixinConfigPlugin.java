package me.colinxu.randomoptimization;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RandomOptimizationMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String MODERN_FIX_COMPAT_MIXIN =
            "me.colinxu.randomoptimization.mixin.compat.ModernFixFilePackResourcesMixin";

    private final Map<String, Boolean> booleanConfigCache = new HashMap<>();
    private boolean modernFixLoaded;

    protected boolean getBooleanConfig(String key){
        return this.booleanConfigCache.computeIfAbsent(key, this::loadBooleanConfig);
    }

    private boolean loadBooleanConfig(String key) {
        return StartupConfig.getBoolean(key);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        if(mixinClassName.equals("me.colinxu.randomoptimization.mixin.ClientMainMixin")){
            return this.getBooleanConfig("lazy_dfu");
        }
        if (mixinClassName.equals("me.colinxu.randomoptimization.mixin.FilePackResourcesMixin")
                || mixinClassName.equals("me.colinxu.randomoptimization.mixin.PathPackResourcesMixin")
                || mixinClassName.equals("me.colinxu.randomoptimization.mixin.SharedZipFileAccessMixin")
                || mixinClassName.equals("me.colinxu.randomoptimization.mixin.VanillaPackResourcesMixin")) {
            return this.getBooleanConfig("optimize_pack_lookup");
        }
        if (mixinClassName.equals(MODERN_FIX_COMPAT_MIXIN)) {
            return this.modernFixLoaded && this.getBooleanConfig("optimize_pack_lookup");
        }
        return true;
    }


    @Override
    public void onLoad(String mixinPackage) {
        this.modernFixLoaded =
                LoadingModList.get().getModFileById("modernfix") != null;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName,ClassNode targetClass,String mixinClassName, IMixinInfo mixinInfo) {}
}
