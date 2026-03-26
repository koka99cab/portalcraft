package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Routes portal teleportation to vanilla Minecraft dimensions.
 *
 * <p>Used as a fallback by {@link ProceduralDimensionService} when procedural
 * dimension creation fails, and as a return-trip router (non-Overworld → Overworld).
 *
 * <h2>Routing rules</h2>
 * <ul>
 *   <li>Non-Overworld source → {@link Level#OVERWORLD}.</li>
 *   <li>Overworld + {@link PortalFamily#END} family → {@link Level#END}.</li>
 *   <li>Overworld + all other families → {@link Level#NETHER}.</li>
 * </ul>
 */
public final class VanillaDimensionService implements PortalDimensionService {

    public static final VanillaDimensionService INSTANCE = new VanillaDimensionService();

    private VanillaDimensionService() {}

    @Override
    public ResourceKey<Level> resolveDimension(PortalIdentity identity, PortalFamily family,
                                                ServerLevel source) {
        if (!source.dimension().equals(Level.OVERWORLD)) {
            return Level.OVERWORLD;
        }
        return switch (family) {
            case END -> Level.END;
            default  -> Level.NETHER;
        };
    }
}
