package dev.botsu.portalcraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compile-time constants shared across the entire mod.
 * Keep this class free of Minecraft API imports so tests can reference it without a game bootstrap.
 */
public final class PortalcraftConstants {

    public static final String MOD_ID = "portalcraft";
    public static final String MOD_NAME = "Portalcraft";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private PortalcraftConstants() {}
}
