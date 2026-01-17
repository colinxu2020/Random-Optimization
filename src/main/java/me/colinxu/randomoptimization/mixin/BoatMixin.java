package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public class BoatMixin {
    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos, CallbackInfo ci) {
        if(Config.fixBoatFallDamage)ci.cancel();
    }
}