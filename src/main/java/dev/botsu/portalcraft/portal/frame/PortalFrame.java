package dev.botsu.portalcraft.portal.frame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Validated, immutable model of a Portalcraft portal frame.
 *
 * <p>A {@code PortalFrame} is only produced by {@link PortalFrameValidator} after both
 * geometric and tag-based validation have passed. It carries everything needed for
 * {@link dev.botsu.portalcraft.portal.analysis.PortalStructureExtractor} to produce a
 * {@link dev.botsu.portalcraft.portal.analysis.PortalStructure} (Stage 1 of the identity
 * pipeline).
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code frameBlocks} — outer contour positions (corners included once each).</li>
 *   <li>{@code innerBlocks} — interior cells (must all have been air at scan time).</li>
 *   <li>{@code axis} — the horizontal axis along which the portal width extends.</li>
 *   <li>{@code innerWidth} — interior width in blocks (2–21).</li>
 *   <li>{@code innerHeight} — interior height in blocks (3–21).</li>
 *   <li>{@code aprilFool} — {@code true} when every frame block belongs to the
 *       {@code portalcraft:april_fool_frame_blocks} tag (wool + full glass).
 *       These portals route to the surreal April Fool dimension family.</li>
 * </ul>
 */
public record PortalFrame(
        List<BlockPos> frameBlocks,
        List<BlockPos> innerBlocks,
        Direction.Axis axis,
        int innerWidth,
        int innerHeight,
        boolean aprilFool
) {

    /** Codec for NBT persistence. */
    public static final Codec<PortalFrame> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockPos.CODEC.listOf().fieldOf("frameBlocks").forGetter(PortalFrame::frameBlocks),
        BlockPos.CODEC.listOf().fieldOf("innerBlocks").forGetter(PortalFrame::innerBlocks),
        Direction.Axis.CODEC.fieldOf("axis").forGetter(PortalFrame::axis),
        Codec.INT.fieldOf("innerWidth").forGetter(PortalFrame::innerWidth),
        Codec.INT.fieldOf("innerHeight").forGetter(PortalFrame::innerHeight),
        Codec.BOOL.fieldOf("aprilFool").forGetter(PortalFrame::aprilFool)
    ).apply(i, PortalFrame::new));

    /** Total number of block positions in the frame. */
    public int frameBlockCount() {
        return frameBlocks.size();
    }

    /** Total number of block positions in the interior. */
    public int innerBlockCount() {
        return innerBlocks.size();
    }
}
