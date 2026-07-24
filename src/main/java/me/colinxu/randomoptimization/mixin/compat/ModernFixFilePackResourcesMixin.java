package me.colinxu.randomoptimization.mixin.compat;

import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents ModernFix's ZIP pack index from shadowing RandomOptimization's
 * prefix-aware index.
 *
 * <p>ModernFix adds this private index factory to {@link FilePackResources}.
 * Returning {@code null} makes its resource-listing callbacks fall through,
 * after which RandomOptimization handles the lookup. This keeps ModernFix's
 * other optimizations enabled and does not modify its configuration.</p>
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
            CallbackInfoReturnable<Object> cir
    ) {
        cir.setReturnValue(null);
    }
}
