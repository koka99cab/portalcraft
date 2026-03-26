package dev.botsu.portalcraft.portal.registry;

import com.mojang.serialization.Codec;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stage 4 of the portal identity pipeline — the persistent dimension registry.
 *
 * <p>Server-side {@link SavedData} stored on the Overworld that maps a
 * {@link PortalIdentity#stableId()} to the {@link DimensionRecord} of the dimension it
 * created. This is the <strong>single source of truth</strong> for the question:
 * "Has this portal recipe ever been used before?"
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>On portal traversal — {@link dev.botsu.portalcraft.portal.dimension.ProceduralDimensionService}
 *       calls {@link #findByIdentity}. If absent, it creates the dimension and calls
 *       {@link #register}.</li>
 *   <li>On Overworld load — {@link dev.botsu.portalcraft.bootstrap.PortalcraftEvents}
 *       calls {@link #getAllRecords()} and recreates each dimension via
 *       {@link dev.botsu.portalcraft.portal.dimension.DynamicDimensionManager}.</li>
 * </ol>
 *
 * <h2>Supersedes</h2>
 * <p>The former {@code DimensionPersistenceData} (removed in P1 cleanup) stored only a flat
 * list of dimension keys. This registry stores richer data and is the canonical identity
 * lookup, keyed on {@code stableId}.
 */
public final class DimensionRegistry extends SavedData {

    static final String ID = "portalcraft_dimension_registry";

    // ── SavedDataType ─────────────────────────────────────────────────────────

    private static final Codec<DimensionRegistry> CODEC =
            DimensionRecord.CODEC.listOf().xmap(
                    list -> {
                        DimensionRegistry r = new DimensionRegistry();
                        for (DimensionRecord rec : list) {
                            r.byStableId.put(rec.stableId(), rec);
                        }
                        return r;
                    },
                    reg -> List.copyOf(reg.byStableId.values())
            );

    public static final SavedDataType<DimensionRegistry> TYPE = new SavedDataType<>(
            ID,
            DimensionRegistry::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    // ── Data ──────────────────────────────────────────────────────────────────

    /** stableId → DimensionRecord. */
    private final Map<Long, DimensionRecord> byStableId = new HashMap<>();

    private DimensionRegistry() {}

    // ── Access ────────────────────────────────────────────────────────────────

    /** Returns (or creates) the registry stored on the Overworld. */
    public static DimensionRegistry getOrCreate(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    /**
     * Looks up the dimension record for the given portal identity.
     *
     * @param identity the Stage-3 portal identity
     * @return the associated record, or empty if this identity has never been seen
     */
    public Optional<DimensionRecord> findByIdentity(PortalIdentity identity) {
        return Optional.ofNullable(byStableId.get(identity.stableId()));
    }

    /**
     * Returns all registered dimension records.
     * Used on server startup to restore dimensions.
     */
    public Collection<DimensionRecord> getAllRecords() {
        return Collections.unmodifiableCollection(byStableId.values());
    }

    // ── Registration / removal ────────────────────────────────────────────────

    /**
     * Removes the dimension record for {@code stableId} and marks the data dirty.
     * No-op if the id is not present.
     *
     * @param stableId the 64-bit portal identity hash
     */
    public void remove(long stableId) {
        if (byStableId.remove(stableId) != null) {
            setDirty();
        }
    }

    /**
     * Registers a newly created dimension. Idempotent — re-registering the same
     * {@code stableId} updates the record and marks dirty.
     *
     * @param identity     the portal identity
     * @param dimensionKey the resource key of the newly created dimension
     * @param family       the portal family (used to select the preset on restart)
     * @return the created {@link DimensionRecord}
     */
    public DimensionRecord register(PortalIdentity identity,
                                    ResourceKey<Level> dimensionKey,
                                    PortalFamily family) {
        DimensionRecord record = new DimensionRecord(
                identity.stableId(),
                identity.canonicalSource(),
                dimensionKey.identifier().getPath(),
                family,
                System.currentTimeMillis() / 1000L
        );
        byStableId.put(identity.stableId(), record);
        setDirty();
        return record;
    }
}
