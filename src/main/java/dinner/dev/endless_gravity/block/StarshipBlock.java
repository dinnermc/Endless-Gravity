package dinner.dev.endless_gravity.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class StarshipBlock extends HorizontalFacingBlock {

    public StarshipBlock(Properties properties, VoxelShape shape) {
        super(properties, shape);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.literal("Gateway to Mars").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}