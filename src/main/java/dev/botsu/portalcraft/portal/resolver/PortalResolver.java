package dev.botsu.portalcraft.portal.resolver;

import dev.botsu.portalcraft.portal.block.PortalColor;
import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.data.PortalRecord;
import dev.botsu.portalcraft.portal.dimension.PortalDimensionService;
import dev.botsu.portalcraft.portal.dimension.ProceduralDimensionService;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Maps a portal's canonical signature and identity to a target dimension and visual color.
 *
 * <h2>Routing</h2>
 * <p>Dimension routing is delegated to a swappable {@link PortalDimensionService}.
 * The default implementation is {@link ProceduralDimensionService}, which consults the
 * {@link dev.botsu.portalcraft.portal.registry.DimensionRegistry} before creating anything.
 *
 * <h2>Color resolution</h2>
 * <p>{@link #resolveColor} maps the dominant portal family to a {@link PortalColor}
 * used to tint the portal block client-side.
 */
public final class PortalResolver {

    private PortalResolver() {}

    /** Active routing strategy — swappable at init time (e.g. for testing). */
    private static volatile PortalDimensionService dimensionService =
        ProceduralDimensionService.INSTANCE;

    /**
     * Replaces the active {@link PortalDimensionService}.
     * Call before any portal is activated (e.g. in mod initialiser).
     */
    public static void setDimensionService(PortalDimensionService service) {
        dimensionService = Objects.requireNonNull(service, "dimensionService must not be null");
    }

    // ── Family resolution ─────────────────────────────────────────────────────

    /**
     * Derives the {@link PortalFamily} from a portal's canonical signature.
     */
    public static PortalFamily resolveFamily(CanonicalPortalSignature canonical) {
        if (canonical.aprilFool()) return PortalFamily.COLOR;
        return switch (canonical.dominantFamily()) {
            case STONE    -> PortalFamily.STONE;
            case SAND     -> PortalFamily.SAND;
            case CLAY     -> PortalFamily.CLAY;
            case MARINE   -> PortalFamily.MARINE;
            case END      -> PortalFamily.END;
            case INFERNAL -> PortalFamily.INFERNAL;
            case VEGETAL  -> PortalFamily.VEGETAL;
            case PRECIOUS -> PortalFamily.PRECIOUS;
            case DARK     -> PortalFamily.DARK;
            case LIGHT    -> PortalFamily.LIGHT;
            case ANCIENT  -> PortalFamily.ANCIENT;
            case ICE      -> PortalFamily.ICE;
            case COLOR    -> PortalFamily.COLOR;
            default       -> PortalFamily.UNKNOWN;
        };
    }

    // ── Dimension resolution ──────────────────────────────────────────────────

    /**
     * Returns the destination dimension for a portal traversal.
     * Delegates to the active {@link PortalDimensionService}.
     *
     * @param record      the portal record (provides identity + canonical)
     * @param sourceLevel the level the portal is in
     * @return the destination dimension key (never null)
     */
    public static ResourceKey<Level> resolveDestination(PortalRecord record,
                                                         ServerLevel sourceLevel) {
        PortalIdentity identity = record.identity();
        PortalFamily family = resolveFamily(record.canonical());
        return dimensionService.resolveDimension(identity, family, sourceLevel);
    }

    // ── Color resolution ──────────────────────────────────────────────────────

    /**
     * Maps the portal's dominant family to a {@link PortalColor} for client-side tinting.
     *
     * @param canonical the canonical portal signature
     * @return the visual color for this portal
     */
    public static PortalColor resolveColor(CanonicalPortalSignature canonical) {
        PortalFamily family = resolveFamily(canonical);
        return switch (family) {
            case SAND, CLAY         -> PortalColor.AMBER;
            case MARINE             -> PortalColor.CYAN;
            case END, PRECIOUS,
                 LIGHT              -> PortalColor.GOLD;
            case VEGETAL            -> PortalColor.GREEN;
            case INFERNAL           -> PortalColor.RED;
            case ICE                -> PortalColor.AZURE;
            case COLOR              -> PortalColor.RAINBOW;
            default                 -> PortalColor.VIOLET; // STONE, DARK, ANCIENT, UNKNOWN
        };
    }

    // ── Coordinate scaling ────────────────────────────────────────────────────

    /**
     * Scales source world coordinates for the dimension transition.
     *
     * <ul>
     *   <li>Overworld → Nether: X/Z ÷ 8 (standard Minecraft coordinate ratio).</li>
     *   <li>Nether → Overworld: X/Z × 8.</li>
     *   <li>All other pairs: coordinates unchanged.</li>
     * </ul>
     *
     * @param pos    the entity's current world position (source dimension)
     * @param source the source dimension key
     * @param dest   the destination dimension key
     * @return scaled XZ position (Y preserved but will be overridden by spawn finder)
     */
    public static Vec3 scaleCoordinates(Vec3 pos, ResourceKey<Level> source,
                                         ResourceKey<Level> dest) {
        if (source.equals(Level.OVERWORLD) && dest.equals(Level.NETHER)) {
            return new Vec3(pos.x / 8.0, pos.y, pos.z / 8.0);
        }
        if (source.equals(Level.NETHER) && dest.equals(Level.OVERWORLD)) {
            return new Vec3(pos.x * 8.0, pos.y, pos.z * 8.0);
        }
        // OW↔End and any custom dimension: keep coordinates (spawn finder picks the Y)
        return pos;
    }
}
