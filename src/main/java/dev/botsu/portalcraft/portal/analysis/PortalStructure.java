package dev.botsu.portalcraft.portal.analysis;

import dev.botsu.portalcraft.portal.frame.PortalFrame;

import java.util.Map;

/**
 * Raw, uninterpreted extraction of a validated portal frame.
 *
 * <p>This is the output of Stage 1 of the identity pipeline. It contains only
 * what was directly observed in the world: the frame geometry and the raw block
 * identifier counts.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>No hashing — the map is raw counts, not yet canonical.</li>
 *   <li>No persistence — this object is ephemeral, created during activation.</li>
 *   <li>No dimension or teleportation logic.</li>
 * </ul>
 */
public record PortalStructure(
        PortalFrame frame,
        Map<String, Integer> rawMaterials
) {

    /** Canonical constructor — defensive copy of the material map. */
    public PortalStructure {
        rawMaterials = Map.copyOf(rawMaterials);
    }

    // Convenience delegates
    public boolean aprilFool() { return frame.aprilFool(); }
}
