package dev.botsu.portalcraft.portal.data;

import com.mojang.serialization.Codec;
import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-dimension persistence layer for active Portalcraft portals.
 *
 * <p>Extends {@link SavedData} so that Minecraft automatically saves and reloads
 * portal records with the world. Each dimension gets its own {@code SavedData}
 * instance stored under {@value #ID} in the dimension's {@code data/} folder.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>On world load — call {@link #populateDataStore} to re-register saved portals
 *       into the in-memory {@link PortalDataStore}.</li>
 *   <li>On portal activation / return-portal build — call {@link #addRecord}.</li>
 *   <li>On portal deactivation — call {@link #removeRecord}.</li>
 *   <li>The game engine calls {@code save()} automatically when the world saves.</li>
 * </ol>
 */
public final class PortalWorldData extends SavedData {

    /** File name used for the saved data (no {@code .dat} extension). */
    static final String ID = "portalcraft_portals";

    // ── SavedDataType (codec-based, replaces old PersistentState.Type) ─────────

    private static final Codec<PortalWorldData> CODEC =
        PortalRecord.CODEC.listOf().xmap(
            list -> {
                PortalWorldData d = new PortalWorldData();
                d.records.addAll(list);
                return d;
            },
            d -> List.copyOf(d.records)
        );

    public static final SavedDataType<PortalWorldData> TYPE = new SavedDataType<>(
        ID,
        PortalWorldData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    // ── Data ──────────────────────────────────────────────────────────────────

    private final List<PortalRecord> records = new ArrayList<>();

    private PortalWorldData() {}

    // ── Access ────────────────────────────────────────────────────────────────

    /**
     * Returns (or creates) the {@code PortalWorldData} for the given dimension.
     */
    public static PortalWorldData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Persists a newly activated portal. Replaces any existing record with the
     * same {@code stableSeed} to avoid duplicates after a world restart.
     */
    public void addRecord(PortalRecord record) {
        records.removeIf(r -> r.stableId() == record.stableId());
        records.add(record);
        setDirty();
    }

    /**
     * Removes a deactivated portal from persistent storage.
     */
    public void removeRecord(PortalRecord record) {
        boolean changed = records.removeIf(r -> r.stableId() == record.stableId());
        if (changed) setDirty();
    }

    // ── World-load bootstrap ──────────────────────────────────────────────────

    /**
     * Re-registers all saved portals in the in-memory {@link PortalDataStore}.
     *
     * <p>Called once per dimension on server-world load. Orphaned records
     * (whose interior blocks no longer exist in the world) are silently discarded
     * and the file is re-saved without them.
     */
    public void populateDataStore(ServerLevel level) {
        List<PortalRecord> orphans = new ArrayList<>();

        for (PortalRecord record : records) {
            if (hasPortalBlocks(level, record)) {
                PortalDataStore.register(level, record);
            } else {
                orphans.add(record);
                PortalcraftConstants.LOGGER.warn(
                    "[Portalcraft] Discarding orphaned portal record (id={}) in {}",
                    Long.toHexString(record.stableId()),
                    level.dimension().identifier());
            }
        }

        if (!orphans.isEmpty()) {
            records.removeAll(orphans);
            setDirty();
        }

        PortalcraftConstants.LOGGER.info(
            "[Portalcraft] Loaded {} portal record(s) in {}",
            records.size() - orphans.size(), level.dimension().identifier());
    }

    /** Returns {@code true} if at least one inner block is still a portal block. */
    private static boolean hasPortalBlocks(ServerLevel level, PortalRecord record) {
        for (BlockPos pos : record.frame().innerBlocks()) {
            if (level.getBlockState(pos).is(ModBlocks.PORTALCRAFT_PORTAL)) {
                return true;
            }
        }
        return false;
    }
}
