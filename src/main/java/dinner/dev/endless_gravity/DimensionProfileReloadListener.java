package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads optional dimension gravity/atmosphere profiles from datapacks under
 * {@code data/<namespace>/endless_gravity/dimensions/*.json}.
 *
 * <p>Profiles defined in {@code config/endless_gravity/dimensions.json} always take
 * precedence over datapack profiles, so server owners and players retain final control.
 */
public final class DimensionProfileReloadListener extends SimpleJsonResourceReloadListener
        implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    public static final String DIRECTORY = "endless_gravity/dimensions";
    private static final ResourceLocation FABRIC_ID =
            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "dimension_profiles");

    public DimensionProfileReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    public ResourceLocation getFabricId() {
        return FABRIC_ID;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Config.CustomDimConfig> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            if (loaded.size() >= Config.MAX_CUSTOM_DIMENSIONS) break;
            ResourceLocation fileId = entry.getKey();
            JsonElement element = entry.getValue();
            if (!element.isJsonObject()) {
                LOGGER.warn("Ignoring datapack dimension profile {}: root must be a JSON object", fileId);
                continue;
            }
            try {
                JsonObject obj = element.getAsJsonObject().deepCopy();
                if (!obj.has("id") && !obj.has("dimension")) {
                    obj.addProperty("id", fileId.toString());
                }
                Config.CustomDimConfig parsed = Config.parseDimensionJson(obj);
                loaded.put(parsed.getDimensionId(), parsed);
            } catch (RuntimeException e) {
                LOGGER.warn("Ignoring invalid datapack dimension profile {}: {}", fileId, e.getMessage());
            }
        }
        Config.setDatapackDimensions(loaded);
        if (!loaded.isEmpty()) {
            LOGGER.info("Loaded {} Endless Gravity dimension profile(s) from datapacks", loaded.size());
        }
    }
}
