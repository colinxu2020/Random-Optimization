package me.colinxu.randomoptimization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.colinxu.randomoptimization.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(Block.class)
public abstract class BlockMixin {
    @WrapOperation(
            method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V"
            )
    )
    private static void randomoptimization$makeDropPredictable(
            Level helperLevel,
            Supplier<ItemEntity> originalSupplier,
            ItemStack helperStack,
            Operation<Void> original,
            Level level,
            BlockPos pos,
            ItemStack stack
    ) {
        if (!Config.predictableItemDrops) {
            original.call(helperLevel, originalSupplier, helperStack);
            return;
        }

        Supplier<ItemEntity> predictableSupplier = () -> {
            ItemEntity itemEntity = originalSupplier.get();
            double itemHalfHeight = EntityType.ITEM.getHeight() / 2.0;
            itemEntity.setPos(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5 - itemHalfHeight,
                    pos.getZ() + 0.5
            );
            itemEntity.setDeltaMovement(0.0, 0.2, 0.0);
            return itemEntity;
        };
        original.call(helperLevel, predictableSupplier, helperStack);
    }
}
