package me.colinxu.randomoptimization;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class RandomOptimizationMixinConfigPlugin implements IMixinConfigPlugin {
    private boolean optimizePackLookup = true;

    @Override
    public void onLoad(String mixinPackage){
        if(net.minecraftforge.fml.ModList.get().isLoaded("quick_pack")){
            this.optimizePackLookup = false;
            return;
        }
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("randomoptimization-common.toml");
        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();
            this.optimizePackLookup = config.getOrElse("optimize_pack_lookup", true);
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        if(mixinClassName.equals("me.colinxu.randomoptimization.mixin.FilePackResourcesMixin")){
            return this.optimizePackLookup;
        }
        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
