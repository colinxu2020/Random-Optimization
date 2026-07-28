package me.colinxu.randomoptimization;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads restart-required settings before Forge creates the normal config values.
 */
public record StartupConfig(
        boolean lazyDfu,
        boolean optimizePackLookup,
        boolean structureLocateSpeedup,
        boolean fixBoatFallDamage
) {
    public static StartupConfig load() {
        return load(FMLPaths.CONFIGDIR.get()
                .resolve("randomoptimization-common.toml"));
    }

    static StartupConfig load(Path configPath) {
        if (!Files.exists(configPath)) {
            return defaults();
        }

        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();
            return new StartupConfig(
                    config.getOrElse("lazy_dfu", true),
                    config.getOrElse("optimize_pack_lookup", true),
                    config.getOrElse("structure_locate_speedup", true),
                    config.getOrElse("fix_boat_fall_damage", true)
            );
        } catch (Exception exception) {
            exception.printStackTrace();
            return defaults();
        }
    }

    private static StartupConfig defaults() {
        return new StartupConfig(true, true, true, true);
    }
}
