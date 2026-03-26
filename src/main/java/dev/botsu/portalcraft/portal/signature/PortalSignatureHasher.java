package dev.botsu.portalcraft.portal.signature;

import java.util.Map;

/**
 * Produces a deterministic 64-bit hash from a portal frame's material composition.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Sort entries by block identifier (lexicographic) to eliminate insertion-order variance.</li>
 *   <li>For each entry, fold the identifier hash and the count into a running hash using a prime multiplier.</li>
 * </ol>
 *
 * <p>This class is intentionally free of Minecraft API imports so it can be
 * exercised by plain JUnit 5 tests without a game bootstrap.
 */
public final class PortalSignatureHasher {

    private PortalSignatureHasher() {}

    /**
     * Computes a deterministic hash for the given material composition.
     *
     * @param materialCounts map of block identifier (e.g. {@code "minecraft:stone"}) to block count
     * @return a 64-bit hash stable across JVM runs (does not rely on {@link Object#hashCode()})
     */
    public static long hash(Map<String, Integer> materialCounts) {
        long h = 17L;
        // Sort by key for a deterministic result regardless of map iteration order.
        for (var entry : materialCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            h = h * 31 + stableStringHash(entry.getKey());
            h = h * 31 + entry.getValue();
        }
        return h;
    }

    /**
     * A simple, stable string hash that does not depend on {@link String#hashCode()}.
     * This ensures the hash is consistent across JVM implementations.
     */
    private static long stableStringHash(String s) {
        long h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = h * 31 + s.charAt(i);
        }
        return h;
    }
}
