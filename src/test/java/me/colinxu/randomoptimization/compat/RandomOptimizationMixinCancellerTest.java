package me.colinxu.randomoptimization.compat;

import me.colinxu.randomoptimization.StartupConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomOptimizationMixinCancellerTest {
    @Test
    void cancelsOnlyEnabledOverlappingFeatures() {
        RandomOptimizationMixinCanceller allEnabled =
                canceller(true, true, true);

        assertTrue(allEnabled.shouldCancel(
                List.of(),
                "bot.inker.forge.shipwreckfix.mixin.Mixin_StructureCheck"
        ));
        assertTrue(allEnabled.shouldCancel(
                List.of(),
                "bot.inker.forge.shipwreckfix.mixin.Mixin_ChunkGenerator"
        ));
        assertTrue(allEnabled.shouldCancel(
                List.of(),
                "elocindev.boatbreakfix.forge.mixin.BoatMixin"
        ));
        assertTrue(allEnabled.shouldCancel(
                List.of(),
                "settingdust.lazyyyyy.mixin.game.pack_resources_cache.PathPackResourcesMixin"
        ));
        assertTrue(allEnabled.shouldCancel(
                List.of(),
                "settingdust.lazyyyyy.mixin.game.v1_20.pack_resources_cache.FilePackResourcesMixin"
        ));
    }

    @Test
    void leavesNonOverlappingAndDisabledFeaturesAlone() {
        RandomOptimizationMixinCanceller allDisabled =
                canceller(false, false, false);
        assertFalse(allDisabled.shouldCancel(
                List.of(),
                "bot.inker.forge.shipwreckfix.mixin.Mixin_StructureCheck"
        ));
        assertFalse(allDisabled.shouldCancel(
                List.of(),
                "elocindev.boatbreakfix.forge.mixin.BoatMixin"
        ));
        assertFalse(allDisabled.shouldCancel(
                List.of(),
                "settingdust.lazyyyyy.mixin.game.pack_resources_cache.PathPackResourcesMixin"
        ));

        RandomOptimizationMixinCanceller allEnabled =
                canceller(true, true, true);
        assertFalse(allEnabled.shouldCancel(
                List.of(),
                "settingdust.lazyyyyy.forge.game.mixin.pack_resources_cache.dynamic_trees.FlatTreeResourcePackMixin"
        ));
        assertFalse(allEnabled.shouldCancel(
                List.of(),
                "other.mod.pack_resources_cache.PathPackResourcesMixin"
        ));
        assertFalse(allEnabled.shouldCancel(
                List.of(),
                "bot.inker.forge.shipwreckfix.mixin.UnrelatedMixin"
        ));
    }

    private static RandomOptimizationMixinCanceller canceller(
            boolean structureLocateSpeedup,
            boolean fixBoatFallDamage,
            boolean optimizePackLookup
    ) {
        return new RandomOptimizationMixinCanceller(new StartupConfig(
                true,
                optimizePackLookup,
                structureLocateSpeedup,
                fixBoatFallDamage
        ));
    }
}
