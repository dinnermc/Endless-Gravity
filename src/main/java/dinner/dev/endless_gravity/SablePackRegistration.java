package dinner.dev.endless_gravity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.nio.file.Path;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public final class SablePackRegistration {

    private SablePackRegistration() {}

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        try {
            if (!net.neoforged.fml.ModList.get().isLoaded("sable")) return;
        } catch (Exception e) {
            return;
        }

        Path datapackRoot = SableDatapackHandler.getDatapackRoot();
        if (datapackRoot == null) return;

        SableDatapackHandler.generateDatapack();

        event.addRepositorySource(consumer -> {
            PackLocationInfo location = new PackLocationInfo(
                    "endless_gravity_generated",
                    Component.literal("Endless Gravity Sable"),
                    PackSource.BUILT_IN,
                    java.util.Optional.empty()
            );

            PackSelectionConfig selection = new PackSelectionConfig(
                    true,
                    Pack.Position.TOP,
                    false
            );

            Pack pack = Pack.readMetaAndCreate(
                    location,
                    new PathPackResources.PathResourcesSupplier(datapackRoot) {
                        @Override
                        public PackResources openPrimary(PackLocationInfo info) {
                            return new PathPackResources(info, datapackRoot);
                        }
                    },
                    PackType.SERVER_DATA,
                    selection
            );

            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
