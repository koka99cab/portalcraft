package dev.botsu.portalcraft;

import dev.botsu.portalcraft.bootstrap.PortalcraftCommands;
import dev.botsu.portalcraft.bootstrap.PortalcraftEvents;
import dev.botsu.portalcraft.portal.activation.DefaultPortalActivationService;
import dev.botsu.portalcraft.portal.activation.PortalActivationService;
import dev.botsu.portalcraft.portal.block.ModBlocks;
import dev.botsu.portalcraft.portal.integration.PortalRuntime;
import dev.botsu.portalcraft.portal.particle.ModParticles;
import dev.botsu.portalcraft.portal.integration.VanillaStylePortalRuntime;
import net.fabricmc.api.ModInitializer;

/**
 * Common (server + client) entrypoint for Portalcraft.
 *
 * <p>Initialises all registries, services, and event hooks in order:
 * <ol>
 *   <li>Block registration ({@link ModBlocks}).</li>
 *   <li>Debug command registration ({@link PortalcraftCommands}).</li>
 *   <li>Portal activation service construction.</li>
 *   <li>Event hook registration ({@link PortalcraftEvents}).</li>
 * </ol>
 */
public class Portalcraft implements ModInitializer {

    /** Active portal runtime (vanilla implementation, no external library required). */
    public static final PortalRuntime PORTAL_RUNTIME = new VanillaStylePortalRuntime();

    /** Activation service orchestrating the full detect → validate → fill pipeline. */
    public static final PortalActivationService ACTIVATION_SERVICE =
        new DefaultPortalActivationService(PORTAL_RUNTIME);

    @Override
    public void onInitialize() {
        PortalcraftConstants.LOGGER.info(
            "{} initializing (Minecraft {}).",
            PortalcraftConstants.MOD_NAME, PortalcraftConstants.MOD_ID);

        ModBlocks.register();
        ModParticles.register();
        PortalcraftCommands.register();
        PortalcraftEvents.register(ACTIVATION_SERVICE);

        PortalcraftConstants.LOGGER.info("{} ready.", PortalcraftConstants.MOD_NAME);
    }
}
