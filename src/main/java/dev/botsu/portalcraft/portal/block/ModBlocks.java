package dev.botsu.portalcraft.portal.block;

import dev.botsu.portalcraft.PortalcraftConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;

/**
 * Registers all Portalcraft blocks.
 *
 * <p>In 1.21.11 the block {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties}
 * must have {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties#setId setId}
 * called before the block is instantiated, so the resource key is constructed here and passed
 * into the block constructor.
 *
 * <p>Call {@link #register()} exactly once from
 * {@link dev.botsu.portalcraft.Portalcraft#onInitialize()}.
 */
public final class ModBlocks {

    /** Registry key for the Portalcraft portal interior block. */
    public static final ResourceKey<Block> PORTALCRAFT_PORTAL_KEY = ResourceKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath(PortalcraftConstants.MOD_ID, "portal")
    );

    /**
     * The custom portal interior block.
     * No item form — created exclusively by {@link dev.botsu.portalcraft.portal.activation.DefaultPortalActivationService}.
     */
    public static final PortalBlock PORTALCRAFT_PORTAL = new PortalBlock(
        Block.Properties.of()
            .setId(PORTALCRAFT_PORTAL_KEY)
            .noCollision()
            .instabreak()
            .lightLevel(state -> 11)
            .sound(SoundType.GLASS)
            .replaceable()
            .pushReaction(PushReaction.DESTROY)
    );

    private ModBlocks() {}

    /** Registers all blocks with the game registry. */
    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, PORTALCRAFT_PORTAL_KEY, PORTALCRAFT_PORTAL);
        PortalcraftConstants.LOGGER.debug("Portalcraft blocks registered.");
    }
}
