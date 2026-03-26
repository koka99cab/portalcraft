package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.registry.DimensionRecord;
import dev.botsu.portalcraft.portal.registry.DimensionRegistry;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Primary {@link PortalDimensionService} — résout les destinations via le
 * {@link DimensionRegistry} persistant et ne crée une dimension que si l'identité
 * n'a jamais été vue.
 *
 * <h2>Algorithme de résolution</h2>
 * <ol>
 *   <li>Cherche l'identité dans le {@link DimensionRegistry}.</li>
 *   <li>Si trouvée :
 *     <ul>
 *       <li>Si la destination == dimension source → c'est un portail retour
 *           (même signature que celui qui a amené le joueur ici) → renvoie vers l'Overworld.</li>
 *       <li>Sinon → renvoie vers la dimension existante. Cela permet la navigation
 *           directe entre dimensions custom (dim_1 → dim_2).</li>
 *     </ul>
 *   </li>
 *   <li>Si absente → crée la dimension, l'enregistre, renvoie la clé.</li>
 *   <li>Si la création échoue → fallback vanilla.</li>
 * </ol>
 *
 * <h2>Invariant garanti</h2>
 * <p>La même {@link PortalIdentity} mène toujours à la même dimension, quelle que
 * soit la dimension source, et y compris après un redémarrage serveur.
 */
public final class ProceduralDimensionService implements PortalDimensionService {

    public static final ProceduralDimensionService INSTANCE = new ProceduralDimensionService();

    private ProceduralDimensionService() {}

    @Override
    public ResourceKey<Level> resolveDimension(PortalIdentity identity, PortalFamily family,
                                                ServerLevel source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            PortalcraftConstants.LOGGER.error(
                "[Portalcraft] Overworld introuvable — fallback vanilla.");
            return VanillaDimensionService.INSTANCE.resolveDimension(identity, family, source);
        }

        DimensionRegistry registry = DimensionRegistry.getOrCreate(overworld);
        Optional<DimensionRecord> existing = registry.findByIdentity(identity);

        if (existing.isPresent()) {
            ResourceKey<Level> destKey = existing.get().dimensionKey();

            // Portail retour : la destination est la dimension où on se trouve déjà.
            // La signature du portail retour est identique à celle du portail d'entrée,
            // donc le registre pointe vers cette même dimension → on renvoie à l'Overworld.
            if (destKey.equals(source.dimension())) {
                PortalcraftConstants.LOGGER.info(
                    "[Portalcraft] Portail retour détecté depuis {} → Overworld",
                    source.dimension().identifier());
                return Level.OVERWORLD;
            }

            // Navigation normale : dim_0 → dim_1 → dim_2 → …
            PortalcraftConstants.LOGGER.info(
                "[Portalcraft] Réutilisation de la dimension {} (depuis {})",
                destKey.identifier(), source.dimension().identifier());
            return destKey;
        }

        // Identité inconnue → première traversée → créer la dimension
        ResourceKey<Level> key = identity.toDimensionKey();
        PortalGenerationPreset preset = FamilyPresetRegistry.getPreset(family);
        ServerLevel created = DynamicDimensionManager.getOrCreate(
            source.getServer(), key, preset,
            identity.stableId(),        // B1: unique terrain seed per portal recipe
            identity.canonicalSource()  // B2: diversity score inferred from canonical string
        );

        if (created != null) {
            registry.register(identity, key, family);
            PortalcraftConstants.LOGGER.info(
                "[Portalcraft] Nouvelle dimension {} créée pour [{}] (depuis {})",
                key.identifier(), identity.canonicalSource(), source.dimension().identifier());
            return key;
        }

        // Création échouée
        PortalcraftConstants.LOGGER.warn(
            "[Portalcraft] Échec création dimension {} → fallback vanilla.", key.identifier());
        return VanillaDimensionService.INSTANCE.resolveDimension(identity, family, source);
    }
}
