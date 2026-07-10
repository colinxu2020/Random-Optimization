package me.colinxu.randomoptimization;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class RandomOptimizationMixinConfigPlugin implements IMixinConfigPlugin {
    private boolean optimizePackLookup = true;
    private boolean lazyDFU = true;

    @Override
    public void onLoad(String mixinPackage){
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("randomoptimization-common.toml");
        if(Files.exists(configPath)) {
            try (FileConfig config = FileConfig.of(configPath)) {
                config.load();
                this.lazyDFU = config.getOrElse("lazy_dfu", true);
                this.optimizePackLookup = config.getOrElse("optimize_pack_lookup", true);
            } catch (Exception e) {
                e.printStackTrace();
                this.lazyDFU = true;
                this.optimizePackLookup = true;
            }
        }else{
            this.lazyDFU = true;
            this.optimizePackLookup = true;
        }
        if(LoadingModList.get().getModFileById("quick_pack") != null){
            this.optimizePackLookup = false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        if(mixinClassName.equals("me.colinxu.randomoptimization.mixin.FilePackResourcesMixin")) {
            return this.optimizePackLookup;
        }
        if(mixinClassName.equals("me.colinxu.randomoptimization.mixin.SharedConstantsMixin")){
            return this.lazyDFU;
        }
        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
