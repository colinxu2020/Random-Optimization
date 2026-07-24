package me.colinxu.randomoptimization;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PREDICTABLE_ITEM_DROPS = BUILDER
            .comment("Whether to make item drop locations predictable")
            .define("predictable_item_drops", true);

    public static final ModConfigSpec.BooleanValue OPTIMIZE_PACK_LOOKUP = BUILDER
            .comment("Whether to optimize resource looking up in compressed packs (GAME RESTART REQUIRED). " +
                    "Automatically disables the overlapping ModernFix and Quick Pack optimizations")
            .define("optimize_pack_lookup", true);

    public static final ModConfigSpec.BooleanValue FIX_BOAT_FALL_DAMAGE = BUILDER
            .comment("Whether to fix boat fall damage")
            .define("fix_boat_fall_damage", true);

    public static final ModConfigSpec.BooleanValue LAZY_DFU = BUILDER
            .comment("Whether to delay the load of DFU")
            .define("lazy_dfu", true);

    static final ModConfigSpec SPEC = BUILDER.build();

}
