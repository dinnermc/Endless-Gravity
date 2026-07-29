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

        generateEndDatapack();
    }

    public static void generateDatapack() {
        try {
            if (!ModList.get().isLoaded("sable")) return;
        } catch (Exception e) {
            return;
        }
        generateEndDatapack();
    }

    private static void generateEndDatapack() {
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

            int priority = Config.COMMON.overworldSableDatapackPriority.get();
            double gravY = Config.COMMON.overworldSableGravityY.get();
            double pressure = Config.COMMON.overworldSablePressure.get();
            double drag = 0.0; // Drag is handled entirely by Endless Gravity mixins

            String json = String.format(Locale.ROOT, """
                    {
                      "dimension": "minecraft:overworld",
                      "priority": %d,
                      "base_gravity": [0.0, %.1f, 0.0],
                      "base_pressure": %.1f,
                      "universal_drag": %.1f,
                      "magnetic_north": [0.0, 0.0, 0.0],
                      "pressure_function": [
                        { "altitude": -64.0,   "value": 1.25,   "slope": -0.001953 },
                        { "altitude": 64.0,    "value": 1.0,    "slope": -0.001488 },
                        { "altitude": 400.0,   "value": 0.5,    "slope": -0.000600 },
                        { "altitude": 900.0,   "value": 0.2,    "slope": -0.000400 },
                        { "altitude": 1200.0,  "value": 0.08,   "slope": -0.000117 },
                        { "altitude": 1800.0,  "value": 0.01,   "slope": -0.000013 },
                        { "altitude": 2500.0,  "value": 0.001,  "slope": -0.000001 },
                        { "altitude": 3500.0,  "value": 0.0,    "slope": 0.0 }
                      ]
                    }""", priority, gravY, pressure, drag);

            if (!Files.exists(jsonPath) || !Files.readString(jsonPath).equals(json)) {
                Files.writeString(jsonPath, json);
            }

            writePackMeta(packMetaPath);

            LOGGER.info("Generated Sable Overworld datapack: gravityY={}, pressure={}, drag={}, priority={}",
                    gravY, pressure, drag, priority);
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
