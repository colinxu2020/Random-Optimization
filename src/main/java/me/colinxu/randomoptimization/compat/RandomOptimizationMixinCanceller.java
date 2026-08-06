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
    private static final String FANCY_MENU_FILE_PACK_RESOURCES_MIXIN =
            "de.keksuccino.fancymenu.mixin.mixins.common.client.MixinFilePackResources";

    private final boolean fixBoatFallDamage;
    private final boolean optimizePackLookup;

    public RandomOptimizationMixinCanceller() {
        this(
                StartupConfig.getBoolean("fix_boat_fall_damage"),
                StartupConfig.getBoolean("optimize_pack_lookup")
        );
    }

    RandomOptimizationMixinCanceller(boolean fixBoatFallDamage) {
        this(fixBoatFallDamage, false);
    }

    RandomOptimizationMixinCanceller(
            boolean fixBoatFallDamage,
            boolean optimizePackLookup
    ) {
        this.fixBoatFallDamage = fixBoatFallDamage;
        this.optimizePackLookup = optimizePackLookup;
    }

    @Override
    public boolean shouldCancel(
            List<String> targetClassNames,
            String mixinClassName
    ) {
        if (this.optimizePackLookup
                && mixinClassName.equals(FANCY_MENU_FILE_PACK_RESOURCES_MIXIN)) {
            return true;
        }

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
