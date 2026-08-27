package com.haiman233.worldtaste.compat;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves Slimefun item IDs while remaining source-independent from optional addons.
 *
 * <p>WorldTaste historically references InfinityExpansion v1 IDs. InfinityExpansion2
 * generally prefixes those IDs with {@code IE_}, but several item families were also
 * renamed. This resolver mirrors the current IE2 legacy migration table, then falls
 * back to the common prefix conversion. No IE2 classes are linked at compile time.</p>
 */
public final class SlimefunItemResolver {

    // Keep this table aligned with InfinityExpansion2 LegacyIdMapper.explicit.
    private static final Map<String, String> IE1_TO_IE2 = Map.ofEntries(
            Map.entry("INFINITE_INGOT", "IE_INFINITY_INGOT"),
            Map.entry("INFINITE_MACHINE_CIRCUIT", "IE_INFINITY_MACHINE_CIRCUIT"),
            Map.entry("INFINITE_MACHINE_CORE", "IE_INFINITY_MACHINE_CORE"),
            Map.entry("END_ESSENCE", "IE_ENDER_ESSENCE"),
            Map.entry("INFINITY_FORGE", "IE_INFINITY_WORKBENCH"),
            Map.entry("BASIC_STRAINER", "IE_STRAINER_1"),
            Map.entry("ADVANCED_STRAINER", "IE_STRAINER_2"),
            Map.entry("REINFORCED_STRAINER", "IE_STRAINER_3"),
            Map.entry("BASIC_COBBLE_GEN", "IE_COBBLESTONE_GENERATOR"),
            Map.entry("ADVANCED_COBBLE_GEN", "IE_COBBLESTONE_GENERATOR_2"),
            Map.entry("INFINITY_COBBLE_GEN", "IE_COBBLESTONE_GENERATOR_4"),
            Map.entry("BASIC_VIRTUAL_FARM", "IE_VIRTUAL_FARM"),
            Map.entry("ADVANCED_VIRTUAL_FARM", "IE_VIRTUAL_FARM_2"),
            Map.entry("INFINITY_VIRTUAL_FARM", "IE_VIRTUAL_FARM_4"),
            Map.entry("BASIC_TREE_GROWER", "IE_TREE_GROWER"),
            Map.entry("ADVANCED_TREE_GROWER", "IE_TREE_GROWER_2"),
            Map.entry("INFINITY_TREE_GROWER", "IE_TREE_GROWER_4"),
            Map.entry("BASIC_QUARRY", "IE_QUARRY"),
            Map.entry("ADVANCED_QUARRY", "IE_QUARRY_2"),
            Map.entry("VOID_QUARRY", "IE_QUARRY_3"),
            Map.entry("INFINITY_QUARRY", "IE_QUARRY_4"),
            Map.entry("INFINITE_VOID_HARVESTER", "IE_VOID_HARVESTER_3"),
            Map.entry("INFINITY_CONSTRUCTOR", "IE_SINGULARITY_CONSTRUCTOR_2"),
            Map.entry("INFINITY_DUST_EXTRACTOR", "IE_DUST_EXTRACTOR_4"),
            Map.entry("INFINITY_INGOT_FORMER", "IE_INGOT_FORMER_4"),
            Map.entry("BASIC_OBSIDIAN_GEN", "IE_OBSIDIAN_GENERATOR"),
            Map.entry("HYDRO_GENERATOR", "IE_HYDRO_GENERATOR"),
            Map.entry("REINFORCED_HYDRO_GENERATOR", "IE_HYDRO_GENERATOR_2"),
            Map.entry("GEOTHERMAL_GENERATOR", "IE_GEOTHERMAL_GENERATOR"),
            Map.entry("REINFORCED_GEOTHERMAL_GENERATOR", "IE_GEOTHERMAL_GENERATOR_2"),
            Map.entry("BASIC_PANEL", "IE_SOLAR_PANEL"),
            Map.entry("ADVANCED_PANEL", "IE_SOLAR_PANEL_2"),
            Map.entry("CELESTIAL_PANEL", "IE_SOLAR_PANEL_3"),
            Map.entry("VOID_PANEL", "IE_VOID_PANEL"),
            Map.entry("INFINITE_PANEL", "IE_INFINITY_PANEL"),
            Map.entry("EMPTY_DATA_CARD", "IE_MOB_DATA_CARD_EMPTY"),
            Map.entry("DATA_INFUSER", "IE_MOB_DATA_INFUSER"),
            Map.entry("BASIC_STORAGE", "IE_STORAGE_UNIT_2"),
            Map.entry("ADVANCED_STORAGE", "IE_STORAGE_UNIT_3"),
            Map.entry("REINFORCED_STORAGE", "IE_STORAGE_UNIT_4"),
            Map.entry("VOID_STORAGE", "IE_STORAGE_UNIT_5"),
            Map.entry("INFINITY_STORAGE", "IE_STORAGE_UNIT_6")
    );

    private static final Map<String, String> IE2_TO_IE1 = reverseMappings();

    private SlimefunItemResolver() {}

    public static SlimefunItem resolve(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;

        String id = rawId.trim().toUpperCase(Locale.ROOT);
        SlimefunItem exact = SlimefunItem.getById(id);
        if (exact != null) return exact;

        for (String candidate : candidatesFor(id)) {
            SlimefunItem item = SlimefunItem.getById(candidate);
            if (item != null) return item;
        }
        return null;
    }

    static Set<String> candidatesFor(String normalizedId) {
        Set<String> candidates = new LinkedHashSet<>();
        if (normalizedId == null || normalizedId.isBlank()) return candidates;

        String id = normalizedId.trim().toUpperCase(Locale.ROOT);
        if (!id.startsWith("IE_")) {
            String mapped = IE1_TO_IE2.get(id);
            if (mapped != null) candidates.add(mapped);

            if (id.endsWith("_DATA_CARD") && !"EMPTY_DATA_CARD".equals(id)) {
                String mob = id.substring(0, id.length() - "_DATA_CARD".length());
                candidates.add("IE_MOB_DATA_CARD_" + mob);
            }
            if (id.startsWith("QUARRY_OSCILLATOR_") && id.length() > "QUARRY_OSCILLATOR_".length()) {
                candidates.add("IE_OSCILLATOR_" + id.substring("QUARRY_OSCILLATOR_".length()));
            }

            // The normal IE1 -> IE2 migration path is FOO -> IE_FOO.
            candidates.add("IE_" + id);
        } else {
            String mapped = IE2_TO_IE1.get(id);
            if (mapped != null) candidates.add(mapped);

            if (id.startsWith("IE_MOB_DATA_CARD_") && !"IE_MOB_DATA_CARD_EMPTY".equals(id)) {
                String mob = id.substring("IE_MOB_DATA_CARD_".length());
                candidates.add(mob + "_DATA_CARD");
            }
            if (id.startsWith("IE_OSCILLATOR_") && id.length() > "IE_OSCILLATOR_".length()) {
                candidates.add("QUARRY_OSCILLATOR_" + id.substring("IE_OSCILLATOR_".length()));
            }

            // Allow IE2-authored content to run against IE1 when names did not change.
            if (id.length() > 3) candidates.add(id.substring(3));
        }

        candidates.remove(id);
        return candidates;
    }

    private static Map<String, String> reverseMappings() {
        Map<String, String> reverse = new LinkedHashMap<>();
        IE1_TO_IE2.forEach((legacy, modern) -> reverse.putIfAbsent(modern, legacy));
        return Map.copyOf(reverse);
    }
}
