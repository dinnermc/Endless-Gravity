package dinner.dev.endless_gravity.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Thread-safe registry for {@link BiomeGravityRule}s from datapacks, addons, and server sync.
 */
public final class BiomeGravityRegistry {

    public static final int MAX_SYNCED_RULES = 512;

    private static final ConcurrentHashMap<String, BiomeGravityRule> API_RULES = new ConcurrentHashMap<>();
    private static volatile Map<String, BiomeGravityRule> datapackRules = Map.of();
    private static volatile Map<String, BiomeGravityRule> syncedRules = Map.of();
    private static volatile boolean useSynced = false;
    private static volatile List<BiomeGravityRule> SNAPSHOT = List.of();

    private BiomeGravityRegistry() {}

    public static void registerApiRule(BiomeGravityRule rule) {
        if (rule == null) throw new IllegalArgumentException("rule must not be null");
        API_RULES.put(rule.id(), rule);
        rebuild();
    }

    public static boolean unregisterApiRule(String id) {
        if (id == null) return false;
        boolean removed = API_RULES.remove(id) != null;
        if (removed) rebuild();
        return removed;
    }

    public static void setDatapackRules(Map<String, BiomeGravityRule> rules) {
        datapackRules = rules == null ? Map.of() : Map.copyOf(rules);
        rebuild();
    }

    public static void setSyncedRules(List<String> encodedRules) {
        Map<String, BiomeGravityRule> map = new LinkedHashMap<>();
        if (encodedRules != null) {
            for (String raw : encodedRules) {
                if (map.size() >= MAX_SYNCED_RULES) break;
                try {
                    BiomeGravityRule rule = BiomeGravityRule.decode(raw);
                    map.put(rule.id(), rule);
                } catch (RuntimeException ignored) {
                }
            }
        }
        syncedRules = map;
        useSynced = true;
        rebuild();
    }

    public static void clearSyncedRules() {
        useSynced = false;
        if (!syncedRules.isEmpty()) {
            syncedRules = Map.of();
            rebuild();
        }
    }

    public static List<String> encodeServerRules() {
        Map<String, BiomeGravityRule> combined = new LinkedHashMap<>(datapackRules);
        combined.putAll(API_RULES);
        if (combined.isEmpty()) return List.of();
        List<String> out = new ArrayList<>(Math.min(combined.size(), MAX_SYNCED_RULES));
        for (BiomeGravityRule rule : combined.values()) {
            if (out.size() >= MAX_SYNCED_RULES) break;
            out.add(rule.encode());
        }
        return out;
    }

    /** Active rules sorted by descending priority (highest priority first). */
    public static List<BiomeGravityRule> activeRules() {
        return SNAPSHOT;
    }

    /** Return the highest-priority rule matching the dimension and biome, or {@code null}. */
    public static BiomeGravityRule match(String dimensionId, String biomeId, Predicate<String> hasBiomeTag) {
        List<BiomeGravityRule> list = SNAPSHOT;
        if (list.isEmpty()) return null;
        for (int i = 0, n = list.size(); i < n; i++) {
            BiomeGravityRule rule = list.get(i);
            if (rule.matches(dimensionId, biomeId, hasBiomeTag)) {
                return rule;
            }
        }
        return null;
    }

    private static void rebuild() {
        Map<String, BiomeGravityRule> merged = new LinkedHashMap<>();
        if (useSynced) {
            merged.putAll(syncedRules);
        } else {
            merged.putAll(datapackRules);
        }
        merged.putAll(API_RULES);
        if (merged.isEmpty()) {
            SNAPSHOT = List.of();
            return;
        }
        List<BiomeGravityRule> list = new ArrayList<>(merged.values());
        // Descending priority; exact biome beats tag on equal priority; then id tiebreaker.
        list.sort(Comparator.comparingInt(BiomeGravityRule::priority).reversed()
                .thenComparing(BiomeGravityRule::isTag)
                .thenComparing(BiomeGravityRule::id));
        SNAPSHOT = List.copyOf(list);
    }
}
