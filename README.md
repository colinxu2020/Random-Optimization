
# Random Optimization

## Description

This mod implements the following features to significantly optimize game performance and fix existing bugs. All features can be toggled via the configuration file.

*   **(1.20.1)Fixes MC-249136:** This bug causes severe server lag (hangs) when players attempt to locate Buried Treasure (most notably), or when opening/breaking chests containing maps, or using the `/locate` command. This mod employs simple checks to drastically reduce the enumeration required during structure searching, thereby optimizing speed.
*   **Deterministic Item Drops:** The mod de-randomizes the spawn position of dropped items. This makes designing Redstone contraptions much simpler and easier. Additionally, by reducing the usage of RNG (Random Number Generation), it improves game performance.
*   **Optimized Resource/Data Pack Loading:**  Improves the loading speed of Resource Packs and Data Packs. The mod Builds a compact index for compressed Resource Packs and Data Packs on first access, replacing repeated full-ZIP scans during namespace discovery and recursive resource listing. This reduces reload time and temporary allocations, especially for large packs and modpacks containing many assets.
*   **Boat Fall Damage Fix:** Fixes an issue where boats take fall damage when falling from specific heights. This aligns behavior with the MC-Wiki description and newer Minecraft versions, while also improving performance.
*   **DFU Optimization:** Defers (lazy loads) the DataFixerUpper (DFU) to improve game startup speed.

## Compatibility & Replacements

This mod completely or partially replaces the functionality of the following mods:

*   **MC-249136 Fix:** This mod fundamentally optimizes the structure search algorithm. In contrast, other mods often move the task to a separate thread to mask the lag. While technically not conflicting, using this mod usually provides sufficient optimization, making the other mod unnecessary. If you strictly require asynchronous loading, **Async Locator** is recommended instead, as it supports more structures and has a better implementation.
*   **Shipwreck/Buried Treasure Fixes:** This mod shares the same essential optimization logic for MC-249136. There is no need to use those mods if this one is installed.
*   **Vertical Fall:** This mod completely replaces all functionality of this mod.
*   **Drizzleproof:** Partially replaced. If you only need deterministic item drops, you do not need Drizzleproof. However, if you require granular configuration for entity mechanics, please continue using Drizzleproof.
*   **Quick Pack:** When this mod's compressed-pack index is enabled, Quick Pack's replacement resource-pack implementation is automatically suppressed and this mod's faster index is used instead.
*   **ModernFix:** When this mod's compressed-pack index is enabled, ModernFix's overlapping ZIP resource-pack index is automatically suppressed. Other ModernFix resource-pack optimizations and all unrelated features remain enabled.
*   **Boat Break Fix:** This mod completely replaces the functionality of this mod.
*   **DFU Loading Optimizations (e.g., LazyDFU):** This mod completely replaces the functionality of these mods.

## Compatibility & License

This mod has good compatibility with other mods and has no known incompatibilities.
It is open-source under the **LGPL-3.0** license.

## Credits

- The [MC-249136](https://github.com/bytzo/mc-249136) by bytzo, which inspired me to speed up the structure locate.
