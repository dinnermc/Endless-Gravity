package dinner.dev.endless_gravity.item;

import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, EndlessGravity.MODID);

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

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::onCreativeTabContents);
    }

    private static void onCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.FUNCTIONAL_BLOCKS) return;
        event.accept(DINNER_PLUSH_ITEM.get());
        event.accept(STARSHIP_ITEM.get());
        event.accept(SUPER_HEAVY_ITEM.get());
        event.accept(STARSHIP_UPPER_ITEM.get());
    }
}