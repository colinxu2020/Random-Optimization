package me.colinxu.randomoptimization.mixin;

import me.colinxu.randomoptimization.ChunkGeneratorStructureStateAccessor;
import me.colinxu.randomoptimization.Config;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Shadow
    @Final
    private ServerChunkCache chunkSource;

    @Shadow
    @Final
    private StructureCheck structureCheck;

    @Inject(method = "<init>",at = @At("RETURN"))
    private void passGeneratorState(CallbackInfo ci) {
        if(!Config.structureLocateSpeedup)return;
        ((ChunkGeneratorStructureStateAccessor) this.structureCheck).randomoptimization$passGeneratorState(this.chunkSource.getGeneratorState());
    }
}
