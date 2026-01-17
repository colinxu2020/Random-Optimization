package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.Config;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;DDD)V", at = @At("TAIL"))
    private void onConstruct(Level pLevel, double pPosX, double pPosY, double pPosZ, ItemStack pStack, double pDeltaX, double pDeltaY, double pDeltaZ, CallbackInfo ci) {
        if(Config.predictableItemDrops) {
            ItemEntity self = (ItemEntity) (Object) this;
            self.setDeltaMovement(0, 0.2, 0);
        }
    }
}
