package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Maps each {@link PortalFamily} to an ordered list of vanilla biomes used in
 * procedural dimension generation.
 *
 * <h2>E — Multi-biome per dimension</h2>
 * <p>Families with more than one biome use a {@link net.minecraft.world.level.biome.MultiNoiseBiomeSource}
 * where the <em>temperature</em> noise axis drives transitions. Biomes are distributed
 * uniformly across the temperature range [−1, +1], ordered from "coldest / emptiest" to
 * "warmest / richest" so that large-scale transitions feel natural.
 *
 * <p>Single-biome families (END, UNKNOWN) fall back to
 * {@link net.minecraft.world.level.biome.FixedBiomeSource} for simplicity.
 *
 * <p>The COLOR family is handled separately by {@link ColorBiomeSelector} — its biome
 * is derived from the HSV colour signature of the portal frame, not from this registry.
 *
 * <h2>Biome ordering rationale</h2>
 * <ul>
 *   <li>STONE    — deep/dark → alive: DEEP_DARK → DRIPSTONE_CAVES → LUSH_CAVES</li>
 *   <li>SAND     — harsh/dry → habitable: ERODED_BADLANDS → DESERT → BADLANDS → SAVANNA</li>
 *   <li>CLAY     — flat → colourful: PLAINS → SAVANNA → SUNFLOWER_PLAINS</li>
 *   <li>MARINE   — frigid → tropical: COLD_OCEAN → DEEP_OCEAN → WARM_OCEAN</li>
 *   <li>INFERNAL — sparse → dense: SOUL_SAND_VALLEY → BASALT_DELTAS → NETHER_WASTES → CRIMSON_FOREST → WARPED_FOREST</li>
 *   <li>VEGETAL  — shaded → tropical: DARK_FOREST → FOREST → FLOWER_FOREST → JUNGLE → BAMBOO_JUNGLE</li>
 *   <li>PRECIOUS — rocky → vibrant: BADLANDS → ERODED_BADLANDS → CHERRY_GROVE</li>
 *   <li>DARK     — lightless → merely gloomy: DEEP_DARK → BASALT_DELTAS → DARK_FOREST</li>
 *   <li>LIGHT    — void → bright: THE_VOID → PLAINS → FLOWER_FOREST</li>
 *   <li>ANCIENT  — deep/old → overgrown: DEEP_DARK → SWAMP → MANGROVE_SWAMP</li>
 *   <li>ICE      — polar → alpine: FROZEN_OCEAN → ICE_SPIKES → SNOWY_PLAINS → GROVE</li>
 * </ul>
 */
public final class FamilyBiomeSetRegistry {

    private FamilyBiomeSetRegistry() {}

    private static final Map<PortalFamily, List<ResourceKey<Biome>>> SETS = buildSets();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the ordered biome list for the given family.
     * The list always contains at least one entry. For the COLOR family, returns
     * a single-element list of {@link Biomes#FLOWER_FOREST} as a safe default —
     * callers should use {@link ColorBiomeSelector} for the actual biome.
     *
     * @param family portal family
     * @return immutable ordered biome list (temperature axis: cold → warm)
     */
    public static List<ResourceKey<Biome>> getBiomes(PortalFamily family) {
        return SETS.getOrDefault(family, SETS.get(PortalFamily.UNKNOWN));
    }

    // ── Registry construction ─────────────────────────────────────────────────

    private static Map<PortalFamily, List<ResourceKey<Biome>>> buildSets() {
        Map<PortalFamily, List<ResourceKey<Biome>>> map = new EnumMap<>(PortalFamily.class);

        // ── STONE — subterranean cave world ──────────────────────────────────
        map.put(PortalFamily.STONE, List.of(
            Biomes.DEEP_DARK,
            Biomes.DRIPSTONE_CAVES,
            Biomes.LUSH_CAVES
        ));

        // ── SAND — arid desert world ─────────────────────────────────────────
        map.put(PortalFamily.SAND, List.of(
            Biomes.ERODED_BADLANDS,
            Biomes.DESERT,
            Biomes.BADLANDS,
            Biomes.SAVANNA
        ));

        // ── CLAY — earthy savanna world ──────────────────────────────────────
        map.put(PortalFamily.CLAY, List.of(
            Biomes.PLAINS,
            Biomes.SAVANNA,
            Biomes.SUNFLOWER_PLAINS
        ));

        // ── MARINE — ocean depths world (ceiling dimension) ──────────────────
        map.put(PortalFamily.MARINE, List.of(
            Biomes.COLD_OCEAN,
            Biomes.DEEP_OCEAN,
            Biomes.WARM_OCEAN
        ));

        // ── END — void world (single biome by design) ────────────────────────
        map.put(PortalFamily.END, List.of(
            Biomes.THE_END
        ));

        // ── INFERNAL — nether world (ceiling dimension) ──────────────────────
        map.put(PortalFamily.INFERNAL, List.of(
            Biomes.SOUL_SAND_VALLEY,
            Biomes.BASALT_DELTAS,
            Biomes.NETHER_WASTES,
            Biomes.CRIMSON_FOREST,
            Biomes.WARPED_FOREST
        ));

        // ── VEGETAL — lush jungle world ──────────────────────────────────────
        map.put(PortalFamily.VEGETAL, List.of(
            Biomes.DARK_FOREST,
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.JUNGLE,
            Biomes.BAMBOO_JUNGLE
        ));

        // ── PRECIOUS — crystal / mineral world ──────────────────────────────
        map.put(PortalFamily.PRECIOUS, List.of(
            Biomes.BADLANDS,
            Biomes.ERODED_BADLANDS,
            Biomes.CHERRY_GROVE
        ));

        // ── DARK — shadow / obsidian world ───────────────────────────────────
        map.put(PortalFamily.DARK, List.of(
            Biomes.DEEP_DARK,
            Biomes.BASALT_DELTAS,
            Biomes.DARK_FOREST
        ));

        // ── LIGHT — luminous / quartz world ──────────────────────────────────
        map.put(PortalFamily.LIGHT, List.of(
            Biomes.THE_VOID,
            Biomes.PLAINS,
            Biomes.FLOWER_FOREST
        ));

        // ── ANCIENT — deepslate / bone world ─────────────────────────────────
        map.put(PortalFamily.ANCIENT, List.of(
            Biomes.DEEP_DARK,
            Biomes.SWAMP,
            Biomes.MANGROVE_SWAMP
        ));

        // ── ICE — frozen / glacial world ─────────────────────────────────────
        map.put(PortalFamily.ICE, List.of(
            Biomes.FROZEN_OCEAN,
            Biomes.ICE_SPIKES,
            Biomes.SNOWY_PLAINS,
            Biomes.GROVE
        ));

        // ── COLOR — computed per-portal by ColorBiomeSelector (safe fallback) ─
        map.put(PortalFamily.COLOR, List.of(
            Biomes.FLOWER_FOREST
        ));

        // ── UNKNOWN — neutral fallback ────────────────────────────────────────
        map.put(PortalFamily.UNKNOWN, List.of(
            Biomes.PLAINS
        ));

        return Map.copyOf(map);
    }
}
