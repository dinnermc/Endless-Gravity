package dinner.dev.endless_gravity.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Biome- or biome-tag-scoped gravity override rule.
 *
 * <p>Can be loaded from datapacks under {@code data/<namespace>/endless_gravity/biomes/*.json}
 * or registered programmatically via {@link GravityApi#registerBiomeRule}.
 */
public final class BiomeGravityRule {

    private static final double VANILLA_LIVING_GRAVITY = 0.08;

    private final String id;
    private final String target;       // exact biome id ("minecraft:plains") or tag ("#c:is_void")
    private final boolean tag;
    private final String targetId;     // without leading '#'
    private final List<String> dimensions; // empty => all dimensions
    private final Double gravityMultiplier; // nullable
    private final Double gravityOffset;     // nullable (living-entity scale)
    private final int priority;

    private BiomeGravityRule(
            String id,
            String target,
            List<String> dimensions,
            Double gravityMultiplier,
            Double gravityOffset,
            int priority) {
        this.id = Objects.requireNonNull(id, "id").trim();
        if (this.id.isEmpty()) throw new IllegalArgumentException("id must not be empty");
        String norm = Objects.requireNonNull(target, "target").trim().toLowerCase(Locale.ROOT);
        if (norm.isEmpty()) throw new IllegalArgumentException("biome target must not be empty");
        this.tag = norm.startsWith("#");
        this.targetId = this.tag ? norm.substring(1) : norm;
        if (this.targetId.isEmpty()) throw new IllegalArgumentException("biome target id must not be empty");
        this.target = norm;
        List<String> dims = new ArrayList<>();
        if (dimensions != null) {
            for (String d : dimensions) {
                if (d != null && !d.isBlank()) {
                    dims.add(d.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.dimensions = List.copyOf(dims);
        if (gravityMultiplier == null && gravityOffset == null) {
            throw new IllegalArgumentException("BiomeGravityRule requires gravity_multiplier or gravity_offset");
        }
        if (gravityMultiplier != null && !Double.isFinite(gravityMultiplier)) {
            throw new IllegalArgumentException("gravity_multiplier must be finite");
        }
        if (gravityOffset != null && !Double.isFinite(gravityOffset)) {
            throw new IllegalArgumentException("gravity_offset must be finite");
        }
        this.gravityMultiplier = gravityMultiplier == null ? null : Math.max(-4.0, Math.min(8.0, gravityMultiplier));
        this.gravityOffset = gravityOffset == null ? null : Math.max(-0.5, Math.min(0.5, gravityOffset));
        this.priority = priority;
    }

    public static BiomeGravityRule byMultiplier(
            String id, String target, List<String> dimensions, double gravityMultiplier, int priority) {
        return new BiomeGravityRule(id, target, dimensions, gravityMultiplier, null, priority);
    }

    public static BiomeGravityRule byOffset(
            String id, String target, List<String> dimensions, double gravityOffset, int priority) {
        return new BiomeGravityRule(id, target, dimensions, null, gravityOffset, priority);
    }

    public String id() { return id; }
    public String target() { return target; }
    public boolean isTag() { return tag; }
    public String targetId() { return targetId; }
    public List<String> dimensions() { return dimensions; }
    public Double gravityMultiplier() { return gravityMultiplier; }
    public Double gravityOffset() { return gravityOffset; }
    public int priority() { return priority; }

    public boolean matchesDimension(String dimensionId) {
        if (dimensions.isEmpty()) return true;
        if (dimensionId == null) return false;
        return dimensions.contains(dimensionId.toLowerCase(Locale.ROOT));
    }

    public boolean matches(String dimensionId, String biomeId, Predicate<String> hasBiomeTag) {
        if (!matchesDimension(dimensionId)) return false;
        if (tag) {
            return hasBiomeTag != null && hasBiomeTag.test(targetId);
        }
        return biomeId != null && targetId.equals(biomeId.toLowerCase(Locale.ROOT));
    }

    /** Compute the upward gravity offset for an entity/channel with the given vanilla base gravity. */
    public double resolveOffset(double baseGravity) {
        if (gravityMultiplier != null) {
            return baseGravity * (1.0 - gravityMultiplier);
        }
        return baseGravity * (gravityOffset / VANILLA_LIVING_GRAVITY);
    }

    /** Parse a datapack JSON object (with {@code fallbackId} when {@code "id"} is omitted). */
    public static BiomeGravityRule fromJson(String fallbackId, JsonObject obj) {
        String id = obj.has("id") ? obj.get("id").getAsString() : fallbackId;
        String target = null;
        if (obj.has("biome_tag")) {
            String rawTag = obj.get("biome_tag").getAsString().trim();
            target = rawTag.startsWith("#") ? rawTag : "#" + rawTag;
        } else if (obj.has("biome")) {
            target = obj.get("biome").getAsString().trim();
        }
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("Missing 'biome' or 'biome_tag'");
        }
        List<String> dims = new ArrayList<>();
        if (obj.has("dimensions") && obj.get("dimensions").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("dimensions");
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) dims.add(el.getAsString());
            }
        } else if (obj.has("dimension") && obj.get("dimension").isJsonPrimitive()) {
            dims.add(obj.get("dimension").getAsString());
        }
        Double mult = obj.has("gravity_multiplier") ? obj.get("gravity_multiplier").getAsDouble() : null;
        Double off = obj.has("gravity_offset") ? obj.get("gravity_offset").getAsDouble() : null;
        int prio = obj.has("priority") ? obj.get("priority").getAsInt() : 0;
        return new BiomeGravityRule(id, target, dims, mult, off, prio);
    }

    /** Compact pipe-separated encoding for server-to-client sync. */
    public String encode() {
        String dims = String.join(",", dimensions);
        String multStr = gravityMultiplier == null ? "" : String.format(Locale.ROOT, "%.6f", gravityMultiplier);
        String offStr = gravityOffset == null ? "" : String.format(Locale.ROOT, "%.6f", gravityOffset);
        return id + "|" + target + "|" + dims + "|" + multStr + "|" + offStr + "|" + priority;
    }

    public static BiomeGravityRule decode(String raw) {
        String[] p = raw.split("\\|", -1);
        if (p.length != 6) {
            throw new IllegalArgumentException("Malformed BiomeGravityRule payload: " + raw);
        }
        String id = p[0];
        String target = p[1];
        List<String> dims = p[2].isEmpty() ? List.of() : List.of(p[2].split(","));
        Double mult = p[3].isEmpty() ? null : Double.parseDouble(p[3]);
        Double off = p[4].isEmpty() ? null : Double.parseDouble(p[4]);
        int prio = Integer.parseInt(p[5]);
        return new BiomeGravityRule(id, target, dims, mult, off, prio);
    }
}
