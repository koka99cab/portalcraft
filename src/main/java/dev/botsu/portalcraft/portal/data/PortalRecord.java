package dev.botsu.portalcraft.portal.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.frame.PortalFrame;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.identity.PortalIdentityDeriver;
import dev.botsu.portalcraft.portal.taxonomy.MaterialFamily;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of an active portal: its geometry and computed canonical signature.
 *
 * <p>Created by {@link dev.botsu.portalcraft.portal.activation.DefaultPortalActivationService}
 * at activation time and stored in {@link PortalDataStore} until the portal is deactivated.
 *
 * <p>The {@link PortalIdentity} is derived on demand from the canonical signature and is
 * not persisted separately — it is always recomputable and fully deterministic.
 */
public record PortalRecord(PortalFrame frame, CanonicalPortalSignature canonical) {

    /** Codec for NBT persistence via {@link PortalWorldData}. Uses the new {@code canonical} field. */
    private static final Codec<PortalRecord> NEW_CODEC = RecordCodecBuilder.create(i -> i.group(
        PortalFrame.CODEC.fieldOf("frame").forGetter(PortalRecord::frame),
        CanonicalPortalSignature.CODEC.fieldOf("canonical").forGetter(PortalRecord::canonical)
    ).apply(i, PortalRecord::new));

    /**
     * Legacy codec that reads the pre-V2 {@code signature} field and converts it to
     * a {@link CanonicalPortalSignature} on the fly.
     *
     * <p>Old format stored: {@code aprilFool, axis (ignored), diversityScore,
     * dominantBlock, dominantFamily, familyCounts, innerHeight, innerWidth, materials, stableSeed (ignored)}.
     * The {@code canonicalString} is reconstructed from the material map and dimensions.
     */
    private static final Codec<PortalRecord> LEGACY_CODEC = RecordCodecBuilder.create(i -> i.group(
        PortalFrame.CODEC.fieldOf("frame").forGetter(PortalRecord::frame),
        legacySignatureCodec().fieldOf("signature").forGetter(PortalRecord::canonical)
    ).apply(i, PortalRecord::new));

    private static Codec<CanonicalPortalSignature> legacySignatureCodec() {
        return RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                .fieldOf("materials").forGetter(CanonicalPortalSignature::sortedMaterials),
            Codec.STRING.fieldOf("dominantBlock")
                .forGetter(CanonicalPortalSignature::dominantBlock),
            MaterialFamily.CODEC.fieldOf("dominantFamily")
                .forGetter(CanonicalPortalSignature::dominantFamily),
            Codec.unboundedMap(MaterialFamily.CODEC, Codec.INT)
                .fieldOf("familyCounts").forGetter(CanonicalPortalSignature::familyCounts),
            Codec.INT.fieldOf("innerWidth").forGetter(CanonicalPortalSignature::innerWidth),
            Codec.INT.fieldOf("innerHeight").forGetter(CanonicalPortalSignature::innerHeight),
            Codec.INT.fieldOf("diversityScore").forGetter(CanonicalPortalSignature::diversityScore),
            Codec.BOOL.fieldOf("aprilFool").forGetter(CanonicalPortalSignature::aprilFool)
        ).apply(i, (materials, dominantBlock, dominantFamily, familyCounts,
                    w, h, diversity, aprilFool) -> {
            String canonical = new TreeMap<>(materials).entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","))
                + ",w:" + w + ",h:" + h;
            return new CanonicalPortalSignature(canonical, materials, dominantBlock,
                dominantFamily, familyCounts, w, h, diversity, aprilFool);
        }));
    }

    /**
     * Public codec: tries the new {@code canonical} format first; falls back to the
     * legacy {@code signature} format for saves created before V2.
     */
    public static final Codec<PortalRecord> CODEC = Codec.withAlternative(NEW_CODEC, LEGACY_CODEC);

    /**
     * Derives the {@link PortalIdentity} from the canonical signature.
     * This is deterministic and free of any world state.
     */
    public PortalIdentity identity() {
        return PortalIdentityDeriver.derive(canonical);
    }

    /** Convenience: the stable 64-bit ID used as the key in {@link PortalDataStore}. */
    public long stableId() {
        return identity().stableId();
    }
}
