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
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(StructureCheck.class)
public class StructureCheckMixin implements ChunkGeneratorStructureStateAccessor {
    @Unique
    private ChunkGeneratorStructureState randomoptimization$generatorState;

    @Unique
    private Map<Structure, List<StructurePlacement>>
            randomoptimization$placementsCache;

    @Unique
    @Override
    public void randomoptimization$passGeneratorState(ChunkGeneratorStructureState state){
        if(this.randomoptimization$generatorState != state){
            this.randomoptimization$generatorState = state;
            this.randomoptimization$placementsCache = null;
        }
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
        ChunkGeneratorStructureState generatorState =
                this.randomoptimization$generatorState;
        List<StructurePlacement> placements =
                this.randomoptimization$getPlacements(
                        instance,
                        pContext,
                        generatorState
                );

        int chunkX = pContext.chunkPos().x;
        int chunkZ = pContext.chunkPos().z;

        for (StructurePlacement placement : placements) {
            if (placement.isStructureChunk(
                    generatorState,
                    chunkX,
                    chunkZ
            )) {
                return original.call(instance, pContext);
            }
        }

        return Optional.empty();
    }

    @Unique
    private List<StructurePlacement> randomoptimization$getPlacements(
            Structure structure,
            Structure.GenerationContext context,
            ChunkGeneratorStructureState generatorState
    ) {
        Map<Structure, List<StructurePlacement>> cache =
                this.randomoptimization$placementsCache;

        if (cache == null) {
            cache = new IdentityHashMap<>(4);
            this.randomoptimization$placementsCache = cache;
        }

        List<StructurePlacement> placements = cache.get(structure);

        if (placements == null) {
            var structureRegistry = context
                    .registryAccess()
                    .registryOrThrow(Registries.STRUCTURE);

            var structureHolder =
                    structureRegistry.wrapAsHolder(structure);

            placements = generatorState
                    .getPlacementsForStructure(structureHolder);

            cache.put(structure, placements);
        }

        return placements;
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
