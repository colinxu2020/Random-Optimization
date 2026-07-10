package me.colinxu.randomoptimization;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = RandomOptimization.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue PREDICTABLE_ITEM_DROPS = BUILDER
            .comment("Whether to make item drop locations predictable")
            .define("predictable_item_drops", true);

    private static final ForgeConfigSpec.BooleanValue STRUCTURE_LOCATE_SPEEDUP = BUILDER
            .comment("Whether to optimize structure locate progress, also fixes MC-249136")
            .define("structure_locate_speedup", true);

    private static final ForgeConfigSpec.BooleanValue OPTIMIZE_PACK_LOOKUP = BUILDER
            .comment("Whether to optimize resource looking up in compressed pack(GAME RESTART REQUIRED)，automatically disabled if Quick Pack is installed")
            .define("optimize_pack_lookup", true);

    private static final ForgeConfigSpec.BooleanValue FIX_BOAT_FALL_DAMAGE = BUILDER
            .comment("Whether to fix boat fall damage")
            .define("fix_boat_fall_damage", true);

    private static final ForgeConfigSpec.BooleanValue LAZY_DFU = BUILDER
            .comment("Whether to delay the load of DFU")
            .define("lazy_dfu", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean predictableItemDrops;
    public static boolean structureLocateSpeedup;
    public static boolean fixBoatFallDamage;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        predictableItemDrops=PREDICTABLE_ITEM_DROPS.get();
        structureLocateSpeedup=STRUCTURE_LOCATE_SPEEDUP.get();
        fixBoatFallDamage=FIX_BOAT_FALL_DAMAGE.get();
    }
}
