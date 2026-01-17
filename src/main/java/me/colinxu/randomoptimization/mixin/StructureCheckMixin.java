package me.colinxu.randomoptimization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.colinxu.randomoptimization.ChunkGeneratorStructureStateAccessor;
import me.colinxu.randomoptimization.Config;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(StructureCheck.class)
public class StructureCheckMixin implements ChunkGeneratorStructureStateAccessor {
    @Unique
    private ChunkGeneratorStructureState randomoptimization$generatorState;

    @Unique
    @Override
    public void randomoptimization$passGeneratorState(ChunkGeneratorStructureState state){
        this.randomoptimization$generatorState = state;
    }

    @WrapOperation(
            method="canCreateStructure(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Z",
            at=@At(value="INVOKE",
                    target="Lnet/minecraft/world/level/levelgen/structure/Structure;findValidGenerationPoint(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;)Ljava/util/Optional;")
    )
    private Optional<Structure.GenerationStub> getGenerationPointIfStructureChunk(Structure instance, Structure.GenerationContext pContext, Operation<Optional<Structure.GenerationStub>> original) {
        if(!Config.structureLocateSpeedup){
            return original.call(instance, pContext);
        }
        var structureHolder=pContext.registryAccess().registryOrThrow(Registries.STRUCTURE).wrapAsHolder(instance);
        if (this.randomoptimization$generatorState != null) {
            for (var structurePlacement : this.randomoptimization$generatorState.getPlacementsForStructure(structureHolder)) {
                if (structurePlacement.isStructureChunk(this.randomoptimization$generatorState, pContext.chunkPos().x, pContext.chunkPos().z)) {
                    return original.call(instance, pContext);
                }
            }
        }
        return Optional.empty();
    }
}
