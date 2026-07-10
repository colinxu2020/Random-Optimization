package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public class BoatMixin {
    @Shadow
    private double lastYd;

    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos, CallbackInfo ci) {
        if(!Config.fixBoatFallDamage)return;
        Boat boat = (Boat) (Object) this;
        this.lastYd = boat.getDeltaMovement().y;
        if(!boat.isPassenger()){
            if(pOnGround)boat.resetFallDistance();
            else if(
                    pY<0.0D &&
                    !boat.canBoatInFluid(boat.level()
                            .getFluidState(boat.blockPosition().below())
                    )
            ){
                boat.fallDistance -= (float)pY;
            }
        }
        ci.cancel();
    }
}