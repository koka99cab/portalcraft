package dev.botsu.portalcraft.portal.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side in-memory registry of all active Portalcraft portals.
 *
 * <p>Two position indices are maintained per dimension:
 * <ul>
 *   <li><b>inner index</b>  — maps every portal interior block position to its record.
 *       Used by {@link dev.botsu.portalcraft.portal.block.PortalBlock} to look up the portal when an entity is inside.</li>
 *   <li><b>frame index</b>  — maps every frame block position to its record.
 *       Used to deactivate the portal when a frame block is broken.</li>
 * </ul>
 *
 * <p>Both indices are cleared atomically by {@link #deactivate} <em>before</em> the
 * interior blocks are replaced with air, which prevents re-entrant deactivation
 * when {@link dev.botsu.portalcraft.portal.block.PortalBlock#onRemove} fires for
 * each removed interior block.
 *
 * <p>Not thread-safe; all accesses must occur on the server thread.
 */
public final class PortalDataStore {

    private PortalDataStore() {}

    /** inner-block → record, per dimension. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, PortalRecord>> INNER = new HashMap<>();
    /** frame-block → record, per dimension. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, PortalRecord>> FRAME = new HashMap<>();
    /** stableSeed → record, per dimension. Used to find the paired portal in the destination. */
    private static final Map<ResourceKey<Level>, Map<Long, PortalRecord>> BY_SEED = new HashMap<>();

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Registers an active portal. All inner and frame positions are indexed.
     */
    public static void register(ServerLevel level, PortalRecord record) {
        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, PortalRecord> inner = INNER.computeIfAbsent(dim, k -> new HashMap<>());
        Map<BlockPos, PortalRecord> frame = FRAME.computeIfAbsent(dim, k -> new HashMap<>());

        for (BlockPos pos : record.frame().innerBlocks()) {
            inner.put(pos.immutable(), record);
        }
        for (BlockPos pos : record.frame().frameBlocks()) {
            frame.put(pos.immutable(), record);
        }
        BY_SEED.computeIfAbsent(dim, k -> new HashMap<>())
               .put(record.stableId(), record);
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    /** Returns the portal whose interior contains {@code pos}, if any. */
    public static Optional<PortalRecord> getByInnerPos(ServerLevel level, BlockPos pos) {
        Map<BlockPos, PortalRecord> index = INNER.get(level.dimension());
        return index == null ? Optional.empty() : Optional.ofNullable(index.get(pos));
    }

    /** Returns the portal whose frame includes {@code pos}, if any. */
    public static Optional<PortalRecord> getByFramePos(ServerLevel level, BlockPos pos) {
        Map<BlockPos, PortalRecord> index = FRAME.get(level.dimension());
        return index == null ? Optional.empty() : Optional.ofNullable(index.get(pos));
    }

    /**
     * Returns the portal with the given {@code stableSeed} in this dimension, if any.
     * Used to find the paired return portal when an entity teleports.
     */
    public static Optional<PortalRecord> getBySeed(ServerLevel level, long seed) {
        Map<Long, PortalRecord> index = BY_SEED.get(level.dimension());
        return index == null ? Optional.empty() : Optional.ofNullable(index.get(seed));
    }

    // ── Deactivation ──────────────────────────────────────────────────────────

    /**
     * Removes all index entries for the given portal.
     *
     * <p>Must be called <em>before</em> replacing interior blocks with air so that
     * the resulting {@code onRemove} callbacks do not re-trigger deactivation.
     */
    public static void deactivate(ServerLevel level, PortalRecord record) {
        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, PortalRecord> inner = INNER.get(dim);
        Map<BlockPos, PortalRecord> frame = FRAME.get(dim);

        if (inner != null) {
            for (BlockPos pos : record.frame().innerBlocks()) {
                inner.remove(pos);
            }
        }
        if (frame != null) {
            for (BlockPos pos : record.frame().frameBlocks()) {
                frame.remove(pos);
            }
        }
        Map<Long, PortalRecord> bySeeds = BY_SEED.get(dim);
        if (bySeeds != null) {
            bySeeds.remove(record.stableId());
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Clears all portals for a given dimension.
     * Should be called when a dimension is unloaded.
     */
    public static void clearDimension(ResourceKey<Level> dim) {
        INNER.remove(dim);
        FRAME.remove(dim);
        BY_SEED.remove(dim);
    }
}
