package me.colinxu.randomoptimization.mixin;

import com.mojang.datafixers.DSL;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(Main.class)
public class ClientMainMixin {
    @Redirect(
            method = "main",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/datafix/DataFixers;optimize(Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;")
    )
    private static CompletableFuture<?> randomoptimization$skipDfuOptimization(
            Set<DSL.TypeReference> references
    ) {
        return CompletableFuture.completedFuture(null);
    }
}
