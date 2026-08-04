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
import java.util.List;
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

        generateEndDatapack();
        generateOverworldDatapack();
    }

    public static void generateDatapack() {
        try {
            if (!ModList.get().isLoaded("sable")) return;
        } catch (Exception e) {
            return;
        }
        generateEndDatapack();
        generateOverworldDatapack();
    }

    private static void generateEndDatapack() {
        try {
            Path dataDir = DATAPACK_ROOT.resolve("data").resolve("endless_gravity").resolve("dimension_physics");
            Path jsonPath = dataDir.resolve("the_end.json");
            Path packMetaPath = DATAPACK_ROOT.resolve("pack.mcmeta");

            Files.createDirectories(dataDir);

            if (!Config.COMMON.endSableGravity.get()) {
                Files.deleteIfExists(jsonPath);
                LOGGER.info("Sable End gravity disabled, removed datapack: {}", jsonPath);
                return;
            }

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

            writePackMeta(packMetaPath);

            LOGGER.info("Generated Sable End datapack: gravityY={}, pressure={}, drag={}, priority={}",
                    gravY, pressure, drag, priority);
        } catch (Exception e) {
            LOGGER.error("Failed to generate Sable End datapack", e);
        }
    }

    private static void generateOverworldDatapack() {
        try {
            Path dataDir = DATAPACK_ROOT.resolve("data").resolve("endless_gravity").resolve("dimension_physics");
            Path jsonPath = dataDir.resolve("overworld.json");
            Path packMetaPath = DATAPACK_ROOT.resolve("pack.mcmeta");

            Files.createDirectories(dataDir);

            if (!Config.COMMON.overworldSableGravity.get()) {
                Files.deleteIfExists(jsonPath);
                LOGGER.info("Sable Overworld gravity disabled, removed datapack: {}", jsonPath);
                return;
            }

            int priority = Config.COMMON.overworldSableDatapackPriority.get();
            List<AtmosphereLayers.Layer> layers = AtmosphereLayers.parse(Config.COMMON.atmosphereLayers.get());

            StringBuilder pressureFunction = new StringBuilder();
            for (int i = 0; i < layers.size(); i++) {
                AtmosphereLayers.Layer layer = layers.get(i);
                if (i > 0) pressureFunction.append(",\n");
                pressureFunction.append(String.format(Locale.ROOT,
                        "                        { \"altitude\": %.1f,   \"value\": %.4f,   \"slope\": %.6f }",
                        layer.altitude(), layer.pressure(), AtmosphereLayers.getSlope(layers, i)));
            }

            String json = String.format(Locale.ROOT, """
                    {
                      "dimension": "minecraft:overworld",
                      "priority": %d,
                      "base_pressure": 1.0,
                      "universal_drag": 0.09,
                      "magnetic_north": [0.0, 0.0, 0.0],
                      "pressure_function": [
                    %s
                      ]
                    }""", priority, pressureFunction);

            if (!Files.exists(jsonPath) || !Files.readString(jsonPath).equals(json)) {
                Files.writeString(jsonPath, json);
            }

            writePackMeta(packMetaPath);

            LOGGER.info("Generated Sable Overworld datapack: layers={}, priority={}",
                    layers.size(), priority);
        } catch (Exception e) {
            LOGGER.error("Failed to generate Sable Overworld datapack", e);
        }
    }

    private static void writePackMeta(Path packMetaPath) throws Exception {
        if (!Files.exists(packMetaPath)) {
            Files.writeString(packMetaPath, """
                    {
                      "pack": {
                        "pack_format": 42,
                        "description": "Endless Gravity Sable Datapack"
                      }
                    }""");
        }
    }
}
