package dev.botsu.portalcraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Tag key constants for Portalcraft block tags.
 *
 * <p>Tag data lives in {@code src/main/resources/data/portalcraft/tags/blocks/}.
 */
public final class PortalcraftTags {

    /**
     * Standard portal frame blocks — stone, terracotta, prismarine, etc.
     * Corresponds to {@code data/portalcraft/tags/blocks/frame_blocks.json}.
     */
    public static final TagKey<Block> FRAME_BLOCKS = TagKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath(PortalcraftConstants.MOD_ID, "frame_blocks")
    );

    /**
     * April Fool portal frame blocks — full-block wool and glass only.
     * When all frame blocks belong to this tag the portal is classified as an April Fool portal.
     * Corresponds to {@code data/portalcraft/tags/blocks/april_fool_frame_blocks.json}.
     */
    public static final TagKey<Block> APRIL_FOOL_FRAME_BLOCKS = TagKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath(PortalcraftConstants.MOD_ID, "april_fool_frame_blocks")
    );

    private PortalcraftTags() {}
}
