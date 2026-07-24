package me.colinxu.randomoptimization.mixin.compat;

import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Restores Minecraft 1.21.1's FilePackResources supplier when Quick Pack is
 * installed, allowing Random Optimization's shared ZIP index to remain active.
 *
 * <p>Quick Pack overwrites this same method at the default priority (1000).
 * This mixin is conditionally enabled by the config plugin and wins the
 * overwrite conflict at priority 1100.</p>
 */
@Mixin(value = FilePackResources.FileResourcesSupplier.class, priority = 1100)
public abstract class QuickPackFileResourcesSupplierMixin {
    @Shadow
    @Final
    private File content;

    /**
     * @author ColinXu
     * @reason Restore the vanilla shared-ZIP/overlay representation for our index.
     */
    @Overwrite
    public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
        FilePackResources.SharedZipFileAccess zipFileAccess =
                new FilePackResources.SharedZipFileAccess(this.content);
        PackResources primary = new FilePackResources(location, zipFileAccess, "");
        List<String> overlays = metadata.overlays();
        if (overlays.isEmpty()) {
            return primary;
        }

        List<PackResources> overlayResources = new ArrayList<>(overlays.size());
        for (String overlay : overlays) {
            overlayResources.add(new FilePackResources(location, zipFileAccess, overlay));
        }
        return new CompositePackResources(primary, overlayResources);
    }
}
