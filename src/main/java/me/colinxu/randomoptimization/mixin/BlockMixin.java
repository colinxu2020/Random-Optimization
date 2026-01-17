package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method="popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"),cancellable = true)
    private static void injectPopResource(Level pLevel, BlockPos pBlock, ItemStack pStack, CallbackInfo ci) {
        if(Config.predictableItemDrops) {
            if(!pLevel.isClientSide()&&!pStack.isEmpty()&&pLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)&&!pLevel.restoringBlockSnapshots) {
                ItemEntity itemEntity = new ItemEntity(pLevel, pBlock.getX() + 0.5D, pBlock.getY() + 0.5D, pBlock.getZ() + 0.5D, pStack);
                itemEntity.setDeltaMovement(0, 0, 0);
                itemEntity.setDefaultPickUpDelay();
                pLevel.addFreshEntity(itemEntity);
            }
            ci.cancel();
        }
    }
}
