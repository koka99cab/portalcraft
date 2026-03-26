package dev.botsu.portalcraft.portal.canonical;

import dev.botsu.portalcraft.portal.analysis.PortalStructure;
import dev.botsu.portalcraft.portal.taxonomy.BlockMaterialClassifier;
import dev.botsu.portalcraft.portal.taxonomy.MaterialFamily;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stage 2 of the portal identity pipeline — canonical normalisation.
 *
 * <p>Transforms a raw {@link PortalStructure} into a deterministic
 * {@link CanonicalPortalSignature}. Two equivalent portals (same blocks, same
 * interior dimensions, any orientation) will always produce the same canonical
 * output.
 *
 * <h2>Normalisation rules</h2>
 * <ol>
 *   <li>Sort the material map by block identifier (lexicographic) — this eliminates
 *       any insertion-order variance from the world scan.</li>
 *   <li>Build the canonical string as
 *       {@code "blockA:count,blockB:count,...,w:W,h:H"} using the sorted entries.</li>
 *   <li>Compute family counts and the dominant family from the sorted map.</li>
 *   <li><strong>Axis is excluded</strong> — portals built in X or Z produce the
 *       same canonical signature.</li>
 * </ol>
 *
 * <h2>Must NOT do</h2>
 * <ul>
 *   <li>Hash or derive a stable ID — that is Stage 3's responsibility.</li>
 *   <li>Access or create dimensions.</li>
 *   <li>Read from or write to the world.</li>
 * </ul>
 */
public final class PortalCanonicalizer {

    private PortalCanonicalizer() {}

    /**
     * Produces the canonical normalised signature for the given portal structure.
     *
     * @param structure the raw structural extraction from Stage 1
     * @return a deterministic {@link CanonicalPortalSignature}
     */
    public static CanonicalPortalSignature canonicalize(PortalStructure structure) {
        // ── 1. Sort materials by block identifier ─────────────────────────────
        Map<String, Integer> sorted = new TreeMap<>(structure.rawMaterials());

        // ── 2. Dominant block ─────────────────────────────────────────────────
        String dominant = sorted.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("minecraft:stone");

        // ── 3. Family counts and dominant family ──────────────────────────────
        Map<MaterialFamily, Integer> familyCounts = new HashMap<>();
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            MaterialFamily fam = BlockMaterialClassifier.classifyById(entry.getKey());
            familyCounts.merge(fam, entry.getValue(), Integer::sum);
        }
        MaterialFamily dominantFamily = familyCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(MaterialFamily.STONE);

        // ── 4. Diversity score ────────────────────────────────────────────────
        int diversityScore = sorted.size();

        // ── 5. Canonical string ───────────────────────────────────────────────
        // Format: "blockA:count,blockB:count,...,w:W,h:H"
        // Axis intentionally excluded.
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        sb.append(",w:").append(structure.frame().innerWidth());
        sb.append(",h:").append(structure.frame().innerHeight());
        String canonicalString = sb.toString();

        return new CanonicalPortalSignature(
                canonicalString,
                sorted,
                dominant,
                dominantFamily,
                familyCounts,
                structure.frame().innerWidth(),
                structure.frame().innerHeight(),
                diversityScore,
                structure.aprilFool()
        );
    }
}
