package me.colinxu.randomoptimization;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
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
    static final String QUICK_PACK_FILE_RESOURCES_SUPPLIER_MIXIN =
            "me.colinxu.randomoptimization.mixin.compat.QuickPackFileResourcesSupplierMixin";

    private boolean optimizePackLookup = true;
    private boolean lazyDFU = true;
    private boolean modernFixLoaded;
    private boolean quickPackLoaded;

    @Override
    public void onLoad(String mixinPackage){
        StartupConfig config = StartupConfig.load();
        this.lazyDFU = config.lazyDfu();
        this.optimizePackLookup = config.optimizePackLookup();
        this.modernFixLoaded = LoadingModList.get().getModFileById("modernfix") != null;
        this.quickPackLoaded = LoadingModList.get().getModFileById("quick_pack") != null;
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
        if(mixinClassName.equals(QUICK_PACK_FILE_RESOURCES_SUPPLIER_MIXIN)) {
            return shouldDisableQuickPackOptimization(
                    this.optimizePackLookup, this.quickPackLoaded);
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

    static boolean shouldDisableQuickPackOptimization(
            boolean optimizePackLookup, boolean quickPackLoaded) {
        // Quick Pack has no feature-level config switch, so neutralize its replacement handler
        // only while our faster FilePackResources implementation is active.
        return optimizePackLookup && quickPackLoaded;
    }

    static boolean neutralizeQuickPackHandler(ClassNode targetClass) {
        for (MethodNode method : targetClass.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0
                    || !method.name.startsWith("modifyReturnValue$")
                    || !method.name.endsWith("$useFastFilePackResources")) {
                continue;
            }

            Type methodType = Type.getMethodType(method.desc);
            Type[] arguments = methodType.getArgumentTypes();
            if (arguments.length != 4 || !methodType.getReturnType().equals(arguments[0])) {
                continue;
            }

            // Quick Pack's first argument is the PackResources returned by vanilla. Replacing
            // the handler body with this two-instruction identity function avoids constructing
            // FastFilePackResources (and therefore avoids opening and closing its ZipFile).
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) {
                method.localVariables.clear();
            }
            if (method.visibleLocalVariableAnnotations != null) {
                method.visibleLocalVariableAnnotations.clear();
            }
            if (method.invisibleLocalVariableAnnotations != null) {
                method.invisibleLocalVariableAnnotations.clear();
            }
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
            method.maxStack = 1;
            return true;
        }
        return false;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {
        if (mixinClassName.equals(QUICK_PACK_FILE_RESOURCES_SUPPLIER_MIXIN)) {
            neutralizeQuickPackHandler(targetClass);
        }
    }
}
