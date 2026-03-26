package dev.botsu.portalcraft.portal.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a COLOR-family portal's canonical source to a vanilla biome based on the
 * weighted HSV colour signature of the wool/stained-glass frame blocks.
 *
 * <h2>Pipeline (D milestone)</h2>
 * <ol>
 *   <li>Parse the canonical source string to extract block IDs and their counts.</li>
 *   <li>Look up each block ID in the built-in dye colour table (H/S/V).</li>
 *   <li>Compute a count-weighted mean:
 *     <ul>
 *       <li><b>Hue</b> — circular mean via unit-circle decomposition (handles 0°/360° wrap).</li>
 *       <li><b>Saturation / Value</b> — arithmetic weighted mean.</li>
 *     </ul>
 *   </li>
 *   <li>Select a biome from an 8-sector × 3-brightness lookup table.</li>
 * </ol>
 *
 * <h2>Colour table</h2>
 * <p>All 16 Minecraft dye colours plus plain glass are represented.
 * Achromatic colours (white, light gray, gray, black — S &lt; 0.15) bypass the
 * hue lookup and route directly to SNOWY_PLAINS / PLAINS / DARK_FOREST / DEEP_DARK
 * based on brightness.
 *
 * <h2>Hue sectors → biomes</h2>
 * <pre>
 *  Sector 0 Red        (H &lt; 20 || H ≥ 330) dark → BADLANDS        mid → SAVANNA        bright → FLOWER_FOREST
 *  Sector 1 Orange     (20 ≤ H &lt; 55)       dark → ERODED_BADLANDS mid → BADLANDS        bright → DESERT
 *  Sector 2 Yellow     (55 ≤ H &lt; 85)       dark → FOREST          mid → SUNFLOWER_PLAINS bright → FLOWER_FOREST
 *  Sector 3 Green      (85 ≤ H &lt; 155)      dark → DARK_FOREST     mid → JUNGLE          bright → BAMBOO_JUNGLE
 *  Sector 4 Teal/Cyan (155 ≤ H &lt; 195)     dark → SWAMP           mid → MANGROVE_SWAMP  bright → WARM_OCEAN
 *  Sector 5 Blue      (195 ≤ H &lt; 255)     dark → COLD_OCEAN      mid → OCEAN           bright → FROZEN_OCEAN
 *  Sector 6 Purple    (255 ≤ H &lt; 310)     dark → DEEP_DARK       mid → DARK_FOREST     bright → CHERRY_GROVE
 *  Sector 7 Pink      (310 ≤ H &lt; 330)     dark → DARK_FOREST     mid → CHERRY_GROVE    bright → CHERRY_GROVE
 * </pre>
 */
public final class ColorBiomeSelector {

    private ColorBiomeSelector() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Selects a biome for a COLOR-family dimension by analysing the dominant hue,
     * saturation, and brightness of the portal frame.
     *
     * @param canonicalSource canonical string from {@link dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature}
     * @return the most thematically appropriate vanilla biome
     */
    public static ResourceKey<Biome> selectBiome(String canonicalSource) {
        Map<String, Integer> materials = parseCanonical(canonicalSource);
        if (materials.isEmpty()) return Biomes.FLOWER_FOREST; // safe default

        // Weighted colour accumulators
        double sinSum = 0, cosSum = 0; // for circular hue mean
        double satSum  = 0, valSum = 0;
        int    totalWeight = 0;
        int    chromaticWeight = 0; // blocks with S >= 0.15

        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            float[] hsv = DYE_HSV.get(entry.getKey());
            if (hsv == null) continue; // non-dye block — ignore

            int weight = entry.getValue();
            float h = hsv[0], s = hsv[1], v = hsv[2];

            satSum  += s * weight;
            valSum  += v * weight;
            totalWeight += weight;

            if (s >= 0.15f) {
                double rad = Math.toRadians(h);
                sinSum += Math.sin(rad) * weight;
                cosSum += Math.cos(rad) * weight;
                chromaticWeight += weight;
            }
        }

        if (totalWeight == 0) return Biomes.FLOWER_FOREST;

        float meanSat = (float) (satSum / totalWeight);
        float meanVal = (float) (valSum / totalWeight);

        // ── Achromatic path (frame is mostly gray/white/black) ─────────────────
        if (meanSat < 0.15f || chromaticWeight == 0) {
            return achromaticBiome(meanVal);
        }

        // ── Chromatic path: circular hue mean ─────────────────────────────────
        float hueDeg = (float) Math.toDegrees(Math.atan2(sinSum, cosSum));
        if (hueDeg < 0) hueDeg += 360f;

        return chromaticBiome(hueDeg, meanVal);
    }

    // ── Biome selection logic ─────────────────────────────────────────────────

    /**
     * Biome lookup for achromatic (low-saturation) palettes.
     * Bright → snowy; mid-tone → plains; dark → deep dark.
     */
    private static ResourceKey<Biome> achromaticBiome(float val) {
        if (val >= 0.75f) return Biomes.SNOWY_PLAINS;   // white, light gray → clean snowy feel
        if (val >= 0.45f) return Biomes.PLAINS;          // gray → neutral
        if (val >= 0.20f) return Biomes.DARK_FOREST;     // dark gray → gloomy
        return Biomes.DEEP_DARK;                          // black → lightless void
    }

    /**
     * Biome lookup for chromatic palettes.
     * The hue selects the 8-sector column; value (brightness) selects the row.
     *
     * @param hueDeg circular hue mean, 0–360
     * @param val    weighted mean value (brightness), 0–1
     */
    private static ResourceKey<Biome> chromaticBiome(float hueDeg, float val) {
        int brightIdx = val >= 0.70f ? 2 : (val >= 0.40f ? 1 : 0);
        int sector    = hueSector(hueDeg);
        return COLOR_BIOME_TABLE[sector][brightIdx];
    }

    /**
     * Maps a hue angle (0–360°) to one of 8 sectors.
     * <pre>
     *   0 = Red        H &lt; 20 || H ≥ 330
     *   1 = Orange     20  ≤ H &lt; 55
     *   2 = Yellow     55  ≤ H &lt; 85
     *   3 = Green      85  ≤ H &lt; 155
     *   4 = Teal/Cyan  155 ≤ H &lt; 195
     *   5 = Blue       195 ≤ H &lt; 255
     *   6 = Purple     255 ≤ H &lt; 310
     *   7 = Pink       310 ≤ H &lt; 330
     * </pre>
     */
    private static int hueSector(float h) {
        if (h < 20 || h >= 330) return 0;  // Red
        if (h < 55)             return 1;  // Orange/Brown
        if (h < 85)             return 2;  // Yellow
        if (h < 155)            return 3;  // Green/Lime
        if (h < 195)            return 4;  // Teal/Cyan
        if (h < 255)            return 5;  // Blue/Light Blue
        if (h < 310)            return 6;  // Purple/Indigo
        return                         7;  // Magenta/Pink
    }

    // ── Biome lookup table [sector 0..7][brightness 0=dark, 1=mid, 2=bright] ─

    @SuppressWarnings("unchecked")
    private static final ResourceKey<Biome>[][] COLOR_BIOME_TABLE = new ResourceKey[][] {
        // Sector 0 — Red
        { Biomes.BADLANDS,       Biomes.SAVANNA,         Biomes.FLOWER_FOREST   },
        // Sector 1 — Orange / Brown
        { Biomes.ERODED_BADLANDS, Biomes.BADLANDS,       Biomes.DESERT          },
        // Sector 2 — Yellow
        { Biomes.FOREST,         Biomes.SUNFLOWER_PLAINS, Biomes.FLOWER_FOREST  },
        // Sector 3 — Green / Lime
        { Biomes.DARK_FOREST,    Biomes.JUNGLE,           Biomes.BAMBOO_JUNGLE  },
        // Sector 4 — Teal / Cyan
        { Biomes.SWAMP,          Biomes.MANGROVE_SWAMP,   Biomes.WARM_OCEAN     },
        // Sector 5 — Blue / Light Blue
        { Biomes.COLD_OCEAN,     Biomes.OCEAN,            Biomes.FROZEN_OCEAN   },
        // Sector 6 — Purple / Indigo
        { Biomes.DEEP_DARK,      Biomes.DARK_FOREST,      Biomes.CHERRY_GROVE   },
        // Sector 7 — Magenta / Pink
        { Biomes.DARK_FOREST,    Biomes.CHERRY_GROVE,     Biomes.CHERRY_GROVE   },
    };

    // ── Dye colour HSV table ──────────────────────────────────────────────────

    /**
     * HSV lookup for all Minecraft dye-coloured blocks (wool + stained glass).
     * {@code float[3] = { hue°, saturation, value }}.
     *
     * <p>Achromatic colours (white, light gray, gray, black) have S=0; they bypass
     * the hue calculation in {@link #selectBiome}.
     */
    private static final Map<String, float[]> DYE_HSV = buildDyeTable();

    private static Map<String, float[]> buildDyeTable() {
        Map<String, float[]> m = new HashMap<>();

        // Plain glass (no dye prefix) — counts as white/achromatic
        float[] whiteHsv = { 0f, 0.00f, 0.95f };
        m.put("minecraft:glass", whiteHsv);

        // Achromatic
        dye(m, "white",      0f,   0.00f, 0.95f);
        dye(m, "light_gray", 0f,   0.00f, 0.65f);
        dye(m, "gray",       0f,   0.00f, 0.40f);
        dye(m, "black",      0f,   0.00f, 0.10f);

        // Warm chromatic
        dye(m, "red",        4f,   0.80f, 0.70f);
        dye(m, "orange",     28f,  0.88f, 0.95f);
        dye(m, "brown",      25f,  0.65f, 0.35f);

        // Light / yellow
        dye(m, "yellow",     48f,  0.76f, 0.99f);

        // Green family
        dye(m, "lime",       83f,  0.85f, 0.78f);
        dye(m, "green",      115f, 0.82f, 0.45f);

        // Cool chromatic
        dye(m, "cyan",       190f, 0.75f, 0.80f);
        dye(m, "light_blue", 205f, 0.55f, 0.95f);
        dye(m, "blue",       228f, 0.82f, 0.55f);

        // Purple / pink family
        dye(m, "purple",     265f, 0.85f, 0.60f);
        dye(m, "magenta",    295f, 0.78f, 0.88f);
        dye(m, "pink",       338f, 0.50f, 0.95f);

        return Map.copyOf(m);
    }

    /**
     * Registers HSV entries for both wool and stained glass variants of a dye colour,
     * plus plain white glass.
     */
    private static void dye(Map<String, float[]> m, String color, float h, float s, float v) {
        float[] hsv = { h, s, v };
        m.put("minecraft:" + color + "_wool",           hsv);
        m.put("minecraft:" + color + "_stained_glass",  hsv);
    }

    // ── Canonical string parser ───────────────────────────────────────────────

    /**
     * Parses {@code "blockA:count,blockB:count,...,w:W,h:H"} into a map of block ID → count.
     * The {@code w:} and {@code h:} tokens are skipped.
     */
    private static Map<String, Integer> parseCanonical(String canonicalSource) {
        if (canonicalSource == null || canonicalSource.isEmpty()) return Map.of();
        Map<String, Integer> result = new HashMap<>();
        for (String part : canonicalSource.split(",")) {
            if (part.startsWith("w:") || part.startsWith("h:")) continue;
            int lastColon = part.lastIndexOf(':');
            if (lastColon < 1) continue; // malformed
            String blockId = part.substring(0, lastColon);
            try {
                int count = Integer.parseInt(part.substring(lastColon + 1));
                if (count > 0) result.put(blockId, count);
            } catch (NumberFormatException ignored) {
                // skip malformed entry
            }
        }
        return result;
    }

}
