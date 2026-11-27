package me.colinxu.randomoptimization.resource;

import net.minecraft.server.packs.PackType;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

/** Publishes each pack type's first result, including an unavailable index. */
public final class PackTypeCache<T> {
    private final AtomicReferenceArray<Optional<T>> entries =
            new AtomicReferenceArray<>(PackType.values().length);

    public T get(PackType type, Supplier<T> load) {
        int slot = type.ordinal();
        Optional<T> entry = entries.get(slot);
        if (entry == null) {
            synchronized (this) {
                entry = entries.get(slot);
                if (entry == null) {
                    entry = Optional.ofNullable(load.get());
                    entries.set(slot, entry);
                }
            }
        }
        return entry.orElse(null);
    }
}
