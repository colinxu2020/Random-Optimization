package me.colinxu.randomoptimization.mixin.compat;

import net.minecraft.server.packs.repository.FolderRepositorySource;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives the config plugin a late application point for disabling Quick Pack's replacement.
 *
 * <p>This mixin deliberately applies after Quick Pack. In {@code postApply}, the plugin replaces
 * Quick Pack's return handler with an identity function that immediately returns its first
 * argument: the original vanilla resource object.</p>
 */
@Mixin(value = FolderRepositorySource.class, priority = 900)
public abstract class QuickPackFileResourcesSupplierMixin {}
