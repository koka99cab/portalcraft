package dev.botsu.portalcraft.portal.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Visible color family of an active Portalcraft portal.
 *
 * <p>Stored as a {@link net.minecraft.world.level.block.state.properties.EnumProperty} on
 * {@link PortalBlock}, allowing the client renderer to tint the portal texture via a
 * {@link net.minecraft.client.color.block.BlockColor} registered in the client initializer.
 *
 * <p>The color is derived from the portal's {@link dev.botsu.portalcraft.portal.taxonomy.MaterialFamily}
 * by {@link dev.botsu.portalcraft.portal.resolver.PortalResolver#resolveColor}.
 *
 * <h2>Color → family mapping</h2>
 * <ul>
 *   <li>{@link #VIOLET}  — STONE, DARK, ANCIENT, UNKNOWN</li>
 *   <li>{@link #AMBER}   — SAND, CLAY</li>
 *   <li>{@link #CYAN}    — MARINE</li>
 *   <li>{@link #GOLD}    — END, PRECIOUS, LIGHT</li>
 *   <li>{@link #GREEN}   — VEGETAL</li>
 *   <li>{@link #RED}     — INFERNAL</li>
 *   <li>{@link #AZURE}   — ICE</li>
 *   <li>{@link #RAINBOW} — COLOR (April Fool wool/glass portals)</li>
 * </ul>
 */
public enum PortalColor implements StringRepresentable {

    VIOLET (0x8B00FF),  // purple  — stone / default
    AMBER  (0xFF8C00),  // orange  — sand / clay
    CYAN   (0x00CED1),  // teal    — marine
    GOLD   (0xFFD700),  // golden  — end / precious / light
    GREEN  (0x228B22),  // forest  — vegetal
    RED    (0xFF2200),  // crimson — infernal
    AZURE  (0x87CEEB),  // ice blue — ice
    RAINBOW(0xFF69B4),  // pink    — april-fool / color
    ;

    /** 0x00RRGGBB packed RGB tint applied by the block colour provider. */
    private final int rgb;
    private final String serializedName;

    PortalColor(int rgb) {
        this.rgb = rgb;
        this.serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int getRgb() { return rgb; }

    @Override
    public String getSerializedName() { return serializedName; }
}
