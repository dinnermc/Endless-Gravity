package dinner.dev.endless_gravity;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class SableDatapackHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** Minecraft 1.21.1 server data pack format. */
    private static final int DATA_PACK_FORMAT = 48;

    public static Path getDatapackRoot() {
        return FMLPaths.CONFIGDIR.get().resolve("endless_gravity_datapack");
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
            Path root = getDatapackRoot();
            Path dataDir = root.resolve("data").resolve("endless_gravity").resolve("dimension_physics");
            Path jsonPath = dataDir.resolve("the_end.json");
            Path packMetaPath = root.resolve("pack.mcmeta");

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
                      "base_gravity": [0.0, %.4f, 0.0],
                      "base_pressure": %.4f,
                      "universal_drag": %.4f,
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
            Path root = getDatapackRoot();
            Path dataDir = root.resolve("data").resolve("endless_gravity").resolve("dimension_physics");
            Path jsonPath = dataDir.resolve("overworld.json");
            Path packMetaPath = root.resolve("pack.mcmeta");

            Files.createDirectories(dataDir);

            // Cosmonautics owns Overworld gravity; delete any pack from an earlier run.
            if (EndlessGravityAPI.isCosmonauticsInstalled()) {
                Files.deleteIfExists(jsonPath);
                LOGGER.info("Create: Cosmonautics detected, removed Sable Overworld datapack: {}", jsonPath);
                return;
            }

            if (!Config.COMMON.overworldSableGravity.get()) {
                Files.deleteIfExists(jsonPath);
                LOGGER.info("Sable Overworld gravity disabled, removed datapack: {}", jsonPath);
                return;
            }

            int priority = Config.COMMON.overworldSableDatapackPriority.get();
            CustomDimConfig overworldProfile = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
            List<AtmosphereLayers.Layer> layers = overworldProfile != null
                    && overworldProfile.getMode() == CustomDimConfig.Mode.ATMOSPHERE
                    ? overworldProfile.getParsedAtmosphereLayers()
                    : AtmosphereLayers.parse(Config.COMMON.atmosphereLayers.get());

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
        String meta = String.format(Locale.ROOT, """
                {
                  "pack": {
                    "pack_format": %d,
                    "description": "Endless Gravity Sable Datapack"
                  }
                }""", DATA_PACK_FORMAT);
        if (!Files.exists(packMetaPath) || !Files.readString(packMetaPath).equals(meta)) {
            Files.writeString(packMetaPath, meta);
        }
    }
}
