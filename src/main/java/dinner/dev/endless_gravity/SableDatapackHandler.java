package dinner.dev.endless_gravity;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class SableDatapackHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Path DATAPACK_ROOT = Paths.get("config", "endless_gravity_datapack");

    public static Path getDatapackRoot() {
        return DATAPACK_ROOT;
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        try {
            if (!ModList.get().isLoaded("sable")) return;
        } catch (Exception e) {
            return;
        }

        generateDatapack();
    }

    public static void generateDatapack() {
        try {
            Path dataDir = DATAPACK_ROOT.resolve("data").resolve("endless_gravity").resolve("dimension_physics");
            Path jsonPath = dataDir.resolve("the_end.json");
            Path packMetaPath = DATAPACK_ROOT.resolve("pack.mcmeta");

            Files.createDirectories(dataDir);

            int priority = Config.COMMON.sableDatapackPriority.get();
            double gravY = Config.COMMON.sableGravityY.get();
            double pressure = Config.COMMON.sablePressure.get();
            double drag = Config.COMMON.sableDrag.get();

            String json = String.format(Locale.ROOT, """
                    {
                      "dimension": "minecraft:the_end",
                      "priority": %d,
                      "base_gravity": [0.0, %.1f, 0.0],
                      "base_pressure": %.1f,
                      "universal_drag": %.1f,
                      "magnetic_north": [0.0, 0.0, 0.0]
                    }""", priority, gravY, pressure, drag);

            if (!Files.exists(jsonPath) || !Files.readString(jsonPath).equals(json)) {
                Files.writeString(jsonPath, json);
            }

            if (!Files.exists(packMetaPath)) {
                Files.writeString(packMetaPath, """
                        {
                          "pack": {
                            "pack_format": 42,
                            "description": "Endless Gravity Sable Datapack"
                          }
                        }""");
            }

            LOGGER.info("Generated Sable datapack: gravityY={}, pressure={}, drag={}, priority={}",
                    gravY, pressure, drag, priority);
        } catch (Exception e) {
            LOGGER.error("Failed to generate Sable datapack", e);
        }
    }
}
