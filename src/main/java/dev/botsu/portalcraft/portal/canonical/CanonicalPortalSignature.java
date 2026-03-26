package dev.botsu.portalcraft.portal.canonical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.botsu.portalcraft.portal.taxonomy.MaterialFamily;

import java.util.Map;

/**
 * Stage 2 of the portal identity pipeline — canonical normalisation.
 *
 * <p>A deterministic, normalised representation of a portal's material composition.
 * Two physically different portals built from the same blocks and dimensions (regardless
 * of orientation) will always produce an identical {@code CanonicalPortalSignature}.
 *
 * <h2>Key properties</h2>
 * <ul>
 *   <li>{@code canonicalString} — a human-readable, lexicographically sorted string
 *       encoding the portal's identity. Format:
 *       {@code "blockA:count,blockB:count,...,w:W,h:H"}.
 *       Example: {@code "minecraft:clay:4,minecraft:obsidian:12,w:3,h:5"}.</li>
 *   <li>{@code sortedMaterials} — the material count map sorted by block identifier.
 *       Input to the Stage-3 hasher.</li>
 *   <li>Portal axis is <strong>intentionally excluded</strong> — the same recipe built in
 *       the X or Z direction routes to the same dimension.</li>
 * </ul>
 *
 * <h2>Must NOT contain</h2>
 * <ul>
 *   <li>A hash or stable ID — that is Stage 3's responsibility.</li>
 *   <li>Any dimension key or world-creation logic.</li>
 *   <li>Any teleportation logic.</li>
 * </ul>
 */
public record CanonicalPortalSignature(
        String canonicalString,
        Map<String, Integer> sortedMaterials,
        String dominantBlock,
        MaterialFamily dominantFamily,
        Map<MaterialFamily, Integer> familyCounts,
        int innerWidth,
        int innerHeight,
        int diversityScore,
        boolean aprilFool
) {

    /** Canonical constructor — defensive copies of all maps. */
    public CanonicalPortalSignature {
        sortedMaterials = Map.copyOf(sortedMaterials);
        familyCounts    = Map.copyOf(familyCounts);
    }

    /** Codec for NBT persistence in {@link dev.botsu.portalcraft.portal.data.PortalRecord}. */
    public static final Codec<CanonicalPortalSignature> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("canonicalString").forGetter(CanonicalPortalSignature::canonicalString),
        Codec.unboundedMap(Codec.STRING, Codec.INT)
            .fieldOf("sortedMaterials").forGetter(CanonicalPortalSignature::sortedMaterials),
        Codec.STRING.fieldOf("dominantBlock").forGetter(CanonicalPortalSignature::dominantBlock),
        MaterialFamily.CODEC.fieldOf("dominantFamily").forGetter(CanonicalPortalSignature::dominantFamily),
        Codec.unboundedMap(MaterialFamily.CODEC, Codec.INT)
            .fieldOf("familyCounts").forGetter(CanonicalPortalSignature::familyCounts),
        Codec.INT.fieldOf("innerWidth").forGetter(CanonicalPortalSignature::innerWidth),
        Codec.INT.fieldOf("innerHeight").forGetter(CanonicalPortalSignature::innerHeight),
        Codec.INT.fieldOf("diversityScore").forGetter(CanonicalPortalSignature::diversityScore),
        Codec.BOOL.fieldOf("aprilFool").forGetter(CanonicalPortalSignature::aprilFool)
    ).apply(i, CanonicalPortalSignature::new));
}
