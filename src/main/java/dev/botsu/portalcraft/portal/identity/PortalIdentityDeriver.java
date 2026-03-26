package dev.botsu.portalcraft.portal.identity;

import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.signature.PortalSignatureHasher;

/**
 * Stage 3 of the portal identity pipeline — stable ID derivation.
 *
 * <p>Converts a {@link CanonicalPortalSignature} into a {@link PortalIdentity} by
 * hashing the sorted material map and mixing in the interior dimensions.
 *
 * <h2>Hash inputs</h2>
 * <ol>
 *   <li>Sorted material counts (block identifier → count) via
 *       {@link PortalSignatureHasher#hash}.</li>
 *   <li>{@code innerWidth} and {@code innerHeight} mixed in with a prime multiplier.</li>
 * </ol>
 *
 * <p><strong>Axis is intentionally excluded</strong> from the hash. A 3×5 obsidian
 * portal built on the X axis and the same portal on the Z axis are considered the
 * same identity and will route to the same dimension.
 *
 * <h2>Must NOT do</h2>
 * <ul>
 *   <li>Read from or write to the world.</li>
 *   <li>Create or look up dimensions.</li>
 *   <li>Perform teleportation logic.</li>
 * </ul>
 */
public final class PortalIdentityDeriver {

    private PortalIdentityDeriver() {}

    /**
     * Derives a stable {@link PortalIdentity} from the given canonical signature.
     *
     * @param canonical the normalised Stage-2 output
     * @return a deterministic, immutable portal identity
     */
    public static PortalIdentity derive(CanonicalPortalSignature canonical) {
        long seed = PortalSignatureHasher.hash(canonical.sortedMaterials());
        seed = seed * 31L + canonical.innerWidth();
        seed = seed * 31L + canonical.innerHeight();
        // axis intentionally excluded — same recipe, any orientation → same destination
        return new PortalIdentity(canonical.canonicalString(), seed, PortalIdentity.CURRENT_VERSION);
    }
}
