package me.colinxu.randomoptimization;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RandomOptimization.MODID)
public class RandomOptimization
{
    public static final String MODID = "randomoptimization";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RandomOptimization() {
        IEventBus modEventBus =
                FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Loading Random Optimizations");
    }
}
