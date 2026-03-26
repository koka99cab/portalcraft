package dev.botsu.portalcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * In-game tests for Portalcraft.
 *
 * <p>These tests are <b>not</b> JUnit tests. They run inside an actual Minecraft
 * instance via the {@code fabric-gametest} entrypoint. Use the {@code /test} command
 * in-game to execute them.
 *
 * <h2>How to add a real game test</h2>
 * <ol>
 *   <li>Annotate a {@code public void} method with {@link GameTest}.</li>
 *   <li>Optionally specify a {@code structure} attribute pointing to an SNBT template
 *       in {@code data/portalcraft/gametest/structure/}. Defaults to an empty 8×8 area
 *       provided by Fabric ({@code "fabric-gametest-api-v1:empty"}).</li>
 *   <li>Use the {@link GameTestHelper} to place blocks, assert states, and simulate
 *       events. Call {@link GameTestHelper#succeed()} when the test passes.</li>
 * </ol>
 */
public class PortalcraftGameTests {

    /**
     * Smoke test — verifies that the {@code fabric-gametest} entrypoint resolves
     * {@code PortalcraftGameTests} successfully. Passes immediately.
     */
    @GameTest
    public void modLoadsCleanly(GameTestHelper helper) {
        // Reaching this method means the entrypoint wired up correctly.
        helper.succeed();
    }
}
