package me.colinxu.randomoptimization.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.colinxu.randomoptimization.Config;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin {
    @Shadow
    @Final
    private Long2ObjectMap<Object2IntMap<Structure>> loadedChunks;

    @Shadow
    @Final
    private long seed;

    @Inject(method = "checkStart", at = @At("HEAD"), cancellable = true)
    private void randomoptimization$rejectImpossibleChunkBeforeStorageScan(
            ChunkPos chunkPos,
            Structure structure,
            StructurePlacement placement,
            boolean skipKnownStructures,
            CallbackInfoReturnable<StructureCheckResult> cir
    ) {
        if (Config.STRUCTURE_LOCATE_SPEEDUP.isTrue()
                && this.loadedChunks.get(chunkPos.toLong()) == null
                && !placement.applyAdditionalChunkRestrictions(
                        chunkPos.x,
                        chunkPos.z,
                        this.seed
                )) {
            cir.setReturnValue(StructureCheckResult.START_NOT_PRESENT);
        }
    }
}
