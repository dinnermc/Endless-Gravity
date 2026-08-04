package dinner.dev.endless_gravity.item;

import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StellarArmorItem extends ArmorItem {

    public static final ResourceLocation SUIT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "textures/models/armor/stellar_suit.png");

    public StellarArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return SUIT_TEXTURE;
    }
}
