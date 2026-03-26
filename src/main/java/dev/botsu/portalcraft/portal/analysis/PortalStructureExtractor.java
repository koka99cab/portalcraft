package dev.botsu.portalcraft.portal.analysis;

import dev.botsu.portalcraft.portal.frame.PortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Stage 1 of the portal identity pipeline — structural extraction.
 *
 * <p>Reads the material composition of a validated {@link PortalFrame} directly
 * from the world and produces a {@link PortalStructure}. This class is the only
 * place that touches {@link ServerLevel} for material reading purposes.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Iterate frame block positions.</li>
 *   <li>Resolve block identifier strings.</li>
 *   <li>Count occurrences per block identifier.</li>
 *   <li>Return an immutable {@link PortalStructure}.</li>
 * </ul>
 *
 * <h2>Must NOT do</h2>
 * <ul>
 *   <li>Hash or normalise the material map.</li>
 *   <li>Persist anything.</li>
 *   <li>Create or look up dimensions.</li>
 *   <li>Perform any teleportation logic.</li>
 * </ul>
 */
public final class PortalStructureExtractor {

    private PortalStructureExtractor() {}

    /**
     * Reads the material counts from all frame block positions in {@code level}
     * and wraps them together with the geometry into a {@link PortalStructure}.
     *
     * @param level the server level to read blocks from
     * @param frame the already-validated portal frame
     * @return the raw structural extraction of the portal
     */
    public static PortalStructure extract(ServerLevel level, PortalFrame frame) {
        Map<String, Integer> counts = new HashMap<>();
        for (BlockPos pos : frame.frameBlocks()) {
            String id = BuiltInRegistries.BLOCK
                    .getKey(level.getBlockState(pos).getBlock())
                    .toString();
            counts.merge(id, 1, Integer::sum);
        }
        return new PortalStructure(frame, counts);
    }
}
