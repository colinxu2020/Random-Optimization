package me.colinxu.randomoptimization.resource;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface NamespaceFilteredPack {
    @Nullable
    List<PackResources> randomoptimization$candidates(PackType type, String namespace,
                                                       Collection<PackResources> children);
}
