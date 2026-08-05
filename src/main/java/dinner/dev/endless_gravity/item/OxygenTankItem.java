package dinner.dev.endless_gravity.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OxygenTankItem extends Item {

    /** Same blue used by the Stellar Chestplate's oxygen bar. */
    public static final int BAR_COLOR = 0x50B7D3;

    private final int capacity;

    public OxygenTankItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Remaining oxygen. A stack clamped at maxDamage - 1 is treated as empty
     * (the item is never destroyed, it just sits in the inventory to recharge).
     */
    public static int getCharge(ItemStack stack) {
        if (!(stack.getItem() instanceof OxygenTankItem item)) return 0;
        int damage = stack.getDamageValue();
        if (damage >= item.capacity - 1) return 0;
        return item.capacity - damage;
    }

    /** Total oxygen left across every tank in the player's main inventory. */
    public static int getInventoryOxygen(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            total += getCharge(stack);
        }
        return total;
    }

    /** The carried tank with the most oxygen remaining, or empty if none has charge. */
    public static ItemStack findBestTank(Player player) {
        ItemStack best = ItemStack.EMPTY;
        int bestCharge = -1;
        for (ItemStack stack : player.getInventory().items) {
            if (!(stack.getItem() instanceof OxygenTankItem)) continue;
            int charge = getCharge(stack);
            if (charge > bestCharge) {
                bestCharge = charge;
                best = stack;
            }
        }
        return best;
    }

    /** Drains up to {@code amount} oxygen from carried tanks. Returns how much was drained. */
    public static int drainFromInventory(Player player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            ItemStack tank = findBestTank(player);
            if (tank.isEmpty()) break;
            int oldCharge = getCharge(tank);
            if (oldCharge <= 0) break;
            int damage = tank.getDamageValue() + Math.min(remaining, oldCharge);
            if (damage >= tank.getMaxDamage()) damage = tank.getMaxDamage() - 1;
            tank.setDamageValue(damage);
            remaining -= oldCharge - getCharge(tank);
        }
        return amount - remaining;
    }

    /** Whether any carried tank is not full. */
    public static boolean hasTankToRecharge(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof OxygenTankItem && stack.getDamageValue() > 0) return true;
        }
        return false;
    }

    /** Recharges every carried tank in full atmosphere. */
    public static void rechargeInventory(Player player, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof OxygenTankItem) {
                int damage = stack.getDamageValue();
                if (damage > 0) {
                    stack.setDamageValue(Math.max(0, damage - amount));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.endless_gravity.oxygen_tank.tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.endless_gravity.oxygen_tank.tooltip.2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.endless_gravity.oxygen_tank.tooltip.3").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("O\u2082: " + getCharge(stack) + " / " + capacity).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if ((float) getCharge(stack) / capacity < 0.25F) return 0xFF4A4A;
        return BAR_COLOR;
    }
}