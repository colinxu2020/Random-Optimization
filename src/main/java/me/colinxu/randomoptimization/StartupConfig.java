package me.colinxu.randomoptimization;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads restart-required settings before NeoForge creates normal config values.
 */
public final class StartupConfig {
    private StartupConfig() {
    }

    public static boolean getBoolean(String key) {
        return getBoolean(
                FMLPaths.CONFIGDIR.get()
                        .resolve("randomoptimization-common.toml"),
                key
        );
    }

    static boolean getBoolean(Path configPath, String key) {
        if (!Files.exists(configPath)) {
            return true;
        }

        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();
            return config.getOrElse(key, true);
        } catch (Exception exception) {
            exception.printStackTrace();
            return true;
        }
    }
}
