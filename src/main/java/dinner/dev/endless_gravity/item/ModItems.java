package dinner.dev.endless_gravity.item;

import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

public class ModItems {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, EndlessGravity.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, EndlessGravity.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EndlessGravity.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STELLAR_MATERIAL =
            ARMOR_MATERIALS.register("stellar", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.CHESTPLATE, 7,
                            ArmorItem.Type.LEGGINGS, 6,
                            ArmorItem.Type.BOOTS, 3
                    ),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "stellar")
                    )),
                    1.0F,
                    0.0F
            ));

    public static final DeferredHolder<Item, StellarArmorItem> STELLAR_HELMET =
            ITEMS.register("stellar_helmet", () -> new StellarArmorItem(
                    STELLAR_MATERIAL,
                    ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(24))
            ));

    public static final DeferredHolder<Item, StellarChestplateItem> STELLAR_CHESTPLATE =
            ITEMS.register("stellar_chestplate", () -> new StellarChestplateItem(
                    STELLAR_MATERIAL,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(24))
            ));

    public static final DeferredHolder<Item, StellarArmorItem> STELLAR_LEGGINGS =
            ITEMS.register("stellar_leggings", () -> new StellarArmorItem(
                    STELLAR_MATERIAL,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(24))
            ));

    public static final DeferredHolder<Item, StellarArmorItem> STELLAR_BOOTS =
            ITEMS.register("stellar_boots", () -> new StellarArmorItem(
                    STELLAR_MATERIAL,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(24))
            ));

    public static final DeferredHolder<Item, OxygenTankItem> OXYGEN_TANK =
            ITEMS.register("oxygen_tank", () -> new OxygenTankItem(
                    new Item.Properties().stacksTo(1).durability(250).setNoRepair(),
                    250
            ));

    public static final DeferredHolder<Item, OxygenTankItem> LARGE_OXYGEN_TANK =
            ITEMS.register("large_oxygen_tank", () -> new OxygenTankItem(
                    new Item.Properties().stacksTo(1).durability(1000).setNoRepair(),
                    1000
            ));

    public static final DeferredHolder<Item, BlockItem> DINNER_PLUSH_ITEM =
            ITEMS.register("dinner_plush", () -> new BlockItem(
                    ModBlocks.DINNER_PLUSH.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> STARSHIP_ITEM =
            ITEMS.register("starship", () -> new BlockItem(
                    ModBlocks.STARSHIP.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> SUPER_HEAVY_ITEM =
            ITEMS.register("super_heavy", () -> new BlockItem(
                    ModBlocks.SUPER_HEAVY.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> STARSHIP_UPPER_ITEM =
            ITEMS.register("starship_upper", () -> new BlockItem(
                    ModBlocks.STARSHIP_UPPER.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENDLESS_GRAVITY_TAB =
            CREATIVE_TABS.register("endless_gravity_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.endless_gravity"))
                            .icon(() -> new ItemStack(STELLAR_HELMET.get()))
                            .displayItems((params, output) -> {
                                output.accept(DINNER_PLUSH_ITEM.get());
                                output.accept(STARSHIP_ITEM.get());
                                output.accept(SUPER_HEAVY_ITEM.get());
                                output.accept(STARSHIP_UPPER_ITEM.get());
                                output.accept(STELLAR_HELMET.get());
                                output.accept(STELLAR_CHESTPLATE.get());
                                output.accept(STELLAR_LEGGINGS.get());
                                output.accept(STELLAR_BOOTS.get());
                                output.accept(OXYGEN_TANK.get());
                                output.accept(LARGE_OXYGEN_TANK.get());
                            })
                            .build());

    public static void init(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}