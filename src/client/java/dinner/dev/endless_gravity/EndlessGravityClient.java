package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.client.model.StellarSuitModel;
import dinner.dev.endless_gravity.client.sound.StarshipEngineSoundController;
import dinner.dev.endless_gravity.item.ModItems;
import dinner.dev.endless_gravity.network.RocketSoundPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class EndlessGravityClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RocketSoundPayload.setClientHandler(StarshipEngineSoundController::handlePacket);
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer()
                .registerExtensionPoint(
                        IConfigScreenFactory.class,
                        (mc, parent) -> EndlessGravityConfigScreen.create(parent)
                );
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StellarSuitModel.STELLAR_SUIT_LAYER, StellarSuitModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions stellarSuit = new IClientItemExtensions() {
            @SuppressWarnings("unchecked")
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(StellarSuitModel.STELLAR_SUIT_LAYER);
                return new StellarSuitModel(root, slot, stack, (HumanoidModel<LivingEntity>) original);
            }

            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                HumanoidModel<?> replacement = getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
                if (replacement != original) {
                    copyModelProperties(original, replacement);
                    return replacement;
                }
                return original;
            }
        };
        event.registerItem(
                stellarSuit,
                ModItems.STELLAR_HELMET.get(),
                ModItems.STELLAR_CHESTPLATE.get(),
                ModItems.STELLAR_LEGGINGS.get(),
                ModItems.STELLAR_BOOTS.get()
        );
    }

    private static <T extends LivingEntity> void copyModelProperties(HumanoidModel<T> from, HumanoidModel<?> to) {
        from.copyPropertiesTo((HumanoidModel<T>) to);
    }
}