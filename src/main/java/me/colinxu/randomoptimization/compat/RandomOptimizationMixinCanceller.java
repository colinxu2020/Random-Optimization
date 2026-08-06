package me.colinxu.randomoptimization.compat;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.mojang.logging.LogUtils;
import me.colinxu.randomoptimization.StartupConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * Prevents other mods from applying only the mixins superseded by enabled RO
 * features. Unrelated functionality in those mods remains active.
 */
public final class RandomOptimizationMixinCanceller implements MixinCanceller {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SHIPWRECK_FIX_PREFIX =
            "bot.inker.forge.shipwreckfix.mixin.";
    private static final Set<String> SHIPWRECK_FIX_MIXINS = Set.of(
            "Mixin_ChunkGenerator",
            "Mixin_StructureCheck",
            "Mixin_StructureManager",
            "Mixin_StructurePlacement"
    );
    private static final String BOAT_BREAK_FIX_MIXIN =
            "elocindev.boatbreakfix.forge.mixin.BoatMixin";
    private static final String FANCY_MENU_FILE_PACK_RESOURCES_MIXIN =
            "de.keksuccino.fancymenu.mixin.mixins.common.client.MixinFilePackResources";
    private static final Set<String> LAZYYYYY_PACK_MIXINS = Set.of(
            "FilePackResourcesMixin",
            "PathPackResourcesMixin",
            "VanillaPackResourcesMixin",
            "ForgePathPackResourcesMixin"
    );

    private final boolean structureLocateSpeedup;
    private final boolean fixBoatFallDamage;
    private final boolean optimizePackLookup;

    public RandomOptimizationMixinCanceller() {
        this(StartupConfig.load());
    }

    RandomOptimizationMixinCanceller(StartupConfig config) {
        this.structureLocateSpeedup = config.structureLocateSpeedup();
        this.fixBoatFallDamage = config.fixBoatFallDamage();
        this.optimizePackLookup = config.optimizePackLookup();
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

        String feature = this.cancelledFeature(mixinClassName);
        if (feature == null) {
            return false;
        }

        LOGGER.info(
                "Disabled overlapping {} mixin {} because Random Optimization's replacement is enabled",
                feature,
                mixinClassName
        );
        return true;
    }

    private String cancelledFeature(String mixinClassName) {
        if (this.structureLocateSpeedup
                && mixinClassName.startsWith(SHIPWRECK_FIX_PREFIX)
                && SHIPWRECK_FIX_MIXINS.contains(
                        mixinClassName.substring(SHIPWRECK_FIX_PREFIX.length())
                )) {
            return "Shipwreck Fix structure-locate";
        }

        if (this.fixBoatFallDamage
                && mixinClassName.equals(BOAT_BREAK_FIX_MIXIN)) {
            return "Boat Break Fix boat-fall";
        }

        if (this.optimizePackLookup
                && mixinClassName.startsWith("settingdust.lazyyyyy.")
                && mixinClassName.contains(".pack_resources_cache.")
                && LAZYYYYY_PACK_MIXINS.contains(simpleName(mixinClassName))) {
            return "lazyyyyy pack-resource cache";
        }

        return null;
    }

    private static String simpleName(String className) {
        int separator = className.lastIndexOf('.');
        return separator < 0 ? className : className.substring(separator + 1);
    }
}
