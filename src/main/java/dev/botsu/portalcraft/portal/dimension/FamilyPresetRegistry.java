package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.portal.block.PortalColor;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps each {@link PortalFamily} to a {@link PortalGenerationPreset}.
 *
 * <p>These presets define the world-generation parameters that Step 7 will use to create
 * custom procedural dimensions. Sky/fog colours, temperature, humidity, terrain scale
 * and block palettes are all derived from the portal's material family.
 *
 * <h2>Colour design rationale</h2>
 * <ul>
 *   <li>Each preset's {@code portalColor} is kept in sync with {@link PortalColor} so the
 *       portal block tint, the destination sky, and the fog all share a consistent hue.</li>
 *   <li>The {@code blockPalette} key is a forward-compatible identifier: in Step 7 it will
 *       resolve to a registered {@code BlockSet} driving surface/filler block selection.</li>
 * </ul>
 */
public final class FamilyPresetRegistry {

    private FamilyPresetRegistry() {}

    private static final Map<PortalFamily, PortalGenerationPreset> PRESETS = buildPresets();

    private static Map<PortalFamily, PortalGenerationPreset> buildPresets() {
        Map<PortalFamily, PortalGenerationPreset> map = new EnumMap<>(PortalFamily.class);

        //                    family          sky        fog        temp  humid scale ceil  sky   palette        color
        add(map, PortalFamily.STONE,   0x777777, 0x444444, 0.5f, 0.30f, 1.5f, false, false, "stone",   PortalColor.VIOLET);
        add(map, PortalFamily.SAND,    0xF5C842, 0xD4902A, 1.8f, 0.00f, 0.4f, false, true,  "desert",  PortalColor.AMBER);
        add(map, PortalFamily.CLAY,    0xD4A96A, 0xBB8844, 1.2f, 0.30f, 0.5f, false, true,  "clay",    PortalColor.AMBER);
        add(map, PortalFamily.MARINE,  0x002080, 0x001050, 0.8f, 1.00f, 0.3f, true,  false, "marine",  PortalColor.CYAN);
        add(map, PortalFamily.END,      0x1A0033, 0x0D0022, 0.5f, 0.00f, 1.0f, false, false, "end",      PortalColor.GOLD);
        add(map, PortalFamily.INFERNAL, 0xFF5500, 0xCC2200, 2.0f, 0.00f, 1.5f, true,  false, "infernal", PortalColor.RED);
        add(map, PortalFamily.VEGETAL,  0x1A5C00, 0x0D3300, 0.8f, 0.90f, 1.1f, false, true,  "jungle",   PortalColor.GREEN);
        add(map, PortalFamily.PRECIOUS,0xBB9900, 0x886600, 0.5f, 0.10f, 1.2f, false, false, "crystal", PortalColor.GOLD);
        add(map, PortalFamily.DARK,    0x0A0A0A, 0x050505, 0.5f, 0.20f, 1.8f, false, false, "dark",    PortalColor.VIOLET);
        add(map, PortalFamily.LIGHT,   0xEEEECC, 0xFFFFFF, 0.5f, 0.30f, 1.0f, false, true,  "light",   PortalColor.GOLD);
        add(map, PortalFamily.ANCIENT, 0x8B6914, 0x5C4209, 0.8f, 0.40f, 1.3f, false, false, "ancient", PortalColor.VIOLET);
        add(map, PortalFamily.ICE,     0xAADDFF, 0xCCEEFF, -0.5f,0.50f, 1.2f, false, true,  "ice",     PortalColor.AZURE);
        add(map, PortalFamily.COLOR,   0xFF80C8, 0xFF40A0, 0.5f, 0.50f, 1.0f, false, true,  "color",   PortalColor.RAINBOW);
        add(map, PortalFamily.UNKNOWN, 0x330033, 0x1A001A, 0.5f, 0.50f, 1.0f, false, false, "stone",   PortalColor.VIOLET);

        return Map.copyOf(map);
    }

    private static void add(Map<PortalFamily, PortalGenerationPreset> map,
                            PortalFamily family, int sky, int fog,
                            float temp, float humid, float scale,
                            boolean ceil, boolean skyLight,
                            String palette, PortalColor color) {
        map.put(family, new PortalGenerationPreset(
            family, sky, fog, temp, humid, scale, ceil, skyLight, palette, color));
    }

    /**
     * Returns the preset for the given family.
     * Falls back to {@link PortalFamily#UNKNOWN} if the family has no explicit entry.
     */
    public static PortalGenerationPreset getPreset(PortalFamily family) {
        return PRESETS.getOrDefault(family, PRESETS.get(PortalFamily.UNKNOWN));
    }
}
