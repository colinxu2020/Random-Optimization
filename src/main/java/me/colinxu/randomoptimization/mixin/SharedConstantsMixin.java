package me.colinxu.randomoptimization.mixin;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SharedConstants.class)
public class SharedConstantsMixin {
    @Inject(method = "enableDataFixerOptimizations", at=@At("HEAD"), cancellable = true)
    private static void enableDataFixerOptimizations(CallbackInfo ci) { ci.cancel(); }
}
