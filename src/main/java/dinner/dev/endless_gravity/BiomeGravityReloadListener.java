package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import dinner.dev.endless_gravity.api.BiomeGravityRegistry;
import dinner.dev.endless_gravity.api.BiomeGravityRule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads optional biome gravity overrides from datapacks under
 * {@code data/<namespace>/endless_gravity/biomes/*.json}.
 */
public final class BiomeGravityReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    public static final String DIRECTORY = "endless_gravity/biomes";

    public BiomeGravityReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, BiomeGravityRule> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            if (loaded.size() >= BiomeGravityRegistry.MAX_SYNCED_RULES) break;
            ResourceLocation fileId = entry.getKey();
            JsonElement element = entry.getValue();
            if (!element.isJsonObject()) {
                LOGGER.warn("Ignoring biome gravity rule {}: root must be a JSON object", fileId);
                continue;
            }
            try {
                BiomeGravityRule rule = BiomeGravityRule.fromJson(fileId.toString(), element.getAsJsonObject());
                loaded.put(rule.id(), rule);
            } catch (RuntimeException e) {
                LOGGER.warn("Ignoring invalid biome gravity rule {}: {}", fileId, e.getMessage());
            }
        }
        BiomeGravityRegistry.setDatapackRules(loaded);
        if (!loaded.isEmpty()) {
            LOGGER.info("Loaded {} Endless Gravity biome rule(s) from datapacks", loaded.size());
        }
    }
}
