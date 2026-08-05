package dinner.dev.endless_gravity.item;

import dinner.dev.endless_gravity.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class StellarChestplateItem extends ArmorItem {

    public static final String TAG_OXYGEN = "endlessgravity_oxygen";
    public static final int BAR_COLOR = 0x50B7D3;

    public StellarChestplateItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    public static int getTank(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_OXYGEN)) return Config.COMMON.oxygenTankCapacity.get();
        return tag.getInt(TAG_OXYGEN);
    }

    public static void setTank(ItemStack stack, int tank) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_OXYGEN, tank);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.endless_gravity.stellar_chestplate.tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.endless_gravity.stellar_chestplate.tooltip.2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.endless_gravity.stellar_chestplate.tooltip.3").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("O\u2082: " + getTank(stack) + " / " + Config.COMMON.oxygenTankCapacity.get()).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Config.COMMON.oxygenTankCapacity.get();
        int tank = getTank(stack);
        return Math.round(13.0F * tank / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int capacity = Config.COMMON.oxygenTankCapacity.get();
        if (capacity <= 0) return BAR_COLOR;
        int tank = getTank(stack);
        if ((float) tank / capacity < 0.25F) return 0xFF4A4A;
        return BAR_COLOR;
    }
}
