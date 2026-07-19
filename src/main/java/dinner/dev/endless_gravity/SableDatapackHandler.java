package dinner.dev.endless_gravity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class SableDatapackHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;
        if (serverLevel.getServer() == null) return;

        try {
            if (!ModList.get().isLoaded("sable")) return;
        } catch (Exception e) {
            return;
        }

        generateDatapack(serverLevel.getServer());
    }

    private static void generateDatapack(net.minecraft.server.MinecraftServer server) {
        try {
            Path worldPath = server.getWorldPath(LevelResource.ROOT);
            Path datapackDir = worldPath.resolve("datapacks").resolve("endless_gravity");
            Path dataDir = datapackDir.resolve("data").resolve("endless_gravity").resolve("dimension_physics");
            Path jsonPath = dataDir.resolve("the_end.json");
            Path packMetaPath = datapackDir.resolve("pack.mcmeta");

            Files.createDirectories(dataDir);

            int priority = Config.COMMON.sableDatapackPriority.get();
            double gravY = Config.COMMON.sableGravityY.get();
            double pressure = Config.COMMON.sablePressure.get();
            double drag = Config.COMMON.sableDrag.get();

            String json = """
                    {
                      "dimension": "minecraft:the_end",
                      "priority": %d,
                      "base_gravity": [0.0, %.1f, 0.0],
                      "base_pressure": %.1f,
                      "universal_drag": %.1f,
                      "magnetic_north": [0.0, 0.0, 0.0]
                    }""".formatted(priority, gravY, pressure, drag);

            if (!Files.exists(jsonPath) || !Files.readString(jsonPath).equals(json)) {
                Files.writeString(jsonPath, json);
            }

            if (!Files.exists(packMetaPath)) {
                Files.writeString(packMetaPath, """
                        {
                          "pack": {
                            "pack_format": 34,
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
