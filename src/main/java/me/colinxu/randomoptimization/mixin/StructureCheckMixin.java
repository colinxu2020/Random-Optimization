package me.colinxu.randomoptimization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DataFixer;
import me.colinxu.randomoptimization.ChunkGeneratorStructureStateAccessor;
import me.colinxu.randomoptimization.Config;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if(!Config.structureLocateSpeedup || this.randomoptimization$generatorState == null){
            return original.call(instance, pContext);
        }
        var structureHolder=pContext.registryAccess().registryOrThrow(Registries.STRUCTURE).wrapAsHolder(instance);
        for (var structurePlacement : this.randomoptimization$generatorState.getPlacementsForStructure(structureHolder)) {
            if (structurePlacement.isStructureChunk(this.randomoptimization$generatorState, pContext.chunkPos().x, pContext.chunkPos().z)) {
                return original.call(instance, pContext);
            }
        }
        return Optional.empty();
    }

    @Inject(method = "<init>", at=@At("RETURN"))
    private void randomoptimization$captureGeneratorState(
            ChunkScanAccess pStorageAccess,
            RegistryAccess pRegistryAccess,
            StructureTemplateManager pStructureTemplateManager,
            ResourceKey<Level> pDimension,
            ChunkGenerator pChunkGenerator,
            RandomState pRandomState,
            LevelHeightAccessor pHeightAccessor,
            BiomeSource pBiomeSource,
            long pSeed,
            DataFixer pFixerUpper,
            CallbackInfo ci
    ) {
        if (!Config.structureLocateSpeedup) {
            return;
        }

        if (pHeightAccessor instanceof ServerLevel serverLevel) {
            this.randomoptimization$generatorState =
                    serverLevel.getChunkSource().getGeneratorState();
        }
    }
}
