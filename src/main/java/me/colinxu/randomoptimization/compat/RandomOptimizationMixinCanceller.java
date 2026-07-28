package me.colinxu.randomoptimization.compat;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.mojang.logging.LogUtils;
import me.colinxu.randomoptimization.StartupConfig;
import org.slf4j.Logger;

import java.util.List;

/**
 * Prevents Boat Break Fix from applying the boat mixin superseded by RO.
 */
public final class RandomOptimizationMixinCanceller implements MixinCanceller {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BOAT_BREAK_FIX_MIXIN =
            "elocindev.boatbreakfix.forge.mixin.BoatMixin";

    private final boolean fixBoatFallDamage;

    public RandomOptimizationMixinCanceller() {
        this(StartupConfig.getBoolean("fix_boat_fall_damage"));
    }

    RandomOptimizationMixinCanceller(boolean fixBoatFallDamage) {
        this.fixBoatFallDamage = fixBoatFallDamage;
    }

    @Override
    public boolean shouldCancel(
            List<String> targetClassNames,
            String mixinClassName
    ) {
        if (!this.fixBoatFallDamage
                || !mixinClassName.equals(BOAT_BREAK_FIX_MIXIN)) {
            return false;
        }

        LOGGER.info(
                "Disabled overlapping Boat Break Fix mixin {} because Random Optimization's replacement is enabled",
                mixinClassName
        );
        return true;
    }
}
