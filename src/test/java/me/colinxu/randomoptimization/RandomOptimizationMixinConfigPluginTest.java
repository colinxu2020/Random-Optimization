package me.colinxu.randomoptimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomOptimizationMixinConfigPluginTest {
    @Test
    void suppressesModernFixOnlyWhenReplacementIsActive() {
        assertTrue(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(true, true));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(false, true));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(true, false));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(false, false));
    }
}
