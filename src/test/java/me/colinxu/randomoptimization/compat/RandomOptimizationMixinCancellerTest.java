package me.colinxu.randomoptimization.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomOptimizationMixinCancellerTest {
    private static final String BOAT_BREAK_FIX_MIXIN =
            "elocindev.boatbreakfix.forge.mixin.BoatMixin";

    @Test
    void cancelsBoatBreakFixOnlyWhenRoBoatFixIsEnabled() {
        assertTrue(new RandomOptimizationMixinCanceller(true)
                .shouldCancel(List.of(), BOAT_BREAK_FIX_MIXIN));
        assertFalse(new RandomOptimizationMixinCanceller(false)
                .shouldCancel(List.of(), BOAT_BREAK_FIX_MIXIN));
    }

    @Test
    void leavesEveryOtherMixinUntouched() {
        RandomOptimizationMixinCanceller canceller =
                new RandomOptimizationMixinCanceller(true);

        assertFalse(canceller.shouldCancel(
                List.of(),
                "elocindev.boatbreakfix.fabric_quilt.mixin.BoatEntityMixin"
        ));
        assertFalse(canceller.shouldCancel(
                List.of(),
                "another.mod.mixin.BoatMixin"
        ));
    }
}
