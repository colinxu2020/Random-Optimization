package me.colinxu.randomoptimization.mixin.compat;

import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents ModernFix's ZIP pack index from shadowing RandomOptimization's faster index.
 *
 * <p>ModernFix adds these callback methods to {@link FilePackResources} from its
 * {@code perf.resourcepacks.FilePackResourcesMixin}. Returning {@code null} from its private
 * index factory makes both callbacks fall through to the underlying Minecraft methods. This
 * is deliberately narrower than overwriting the Minecraft methods again: Fusion and other
 * resource-pack decorators remain free to operate, and no ModernFix configuration file is
 * modified.</p>
 */
@Mixin(value = FilePackResources.class, priority = 1100)
public abstract class ModernFixFilePackResourcesMixin {
    @Dynamic("Index factory added by ModernFix")
    @Inject(
            method = "mf$getOrCreateIndex",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void randomoptimization$disableModernFixIndex(
            CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(null);
    }
}
