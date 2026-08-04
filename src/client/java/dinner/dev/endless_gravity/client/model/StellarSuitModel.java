package dinner.dev.endless_gravity.client.model;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class StellarSuitModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation STELLAR_SUIT_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "stellar_suit"), "main");
    public static final ResourceLocation STELLAR_SUIT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "textures/models/armor/stellar_suit.png");

    private final ModelPart visor;
    private final ModelPart belt;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    private final EquipmentSlot slot;
    @Nullable
    private final HumanoidModel<LivingEntity> parentModel;

    public StellarSuitModel(ModelPart root, EquipmentSlot slot, ItemStack stack, @Nullable HumanoidModel<LivingEntity> parentModel) {
        super(root, RenderType::entityTranslucent);
        this.visor = root.getChild("visor");
        this.belt = root.getChild("belt");
        this.rightBoot = root.getChild("right_boot");
        this.leftBoot = root.getChild("left_boot");
        this.slot = slot;
        this.parentModel = parentModel;
        this.setVisible();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        buffer = bufferSource.getBuffer(RenderType.entityTranslucent(STELLAR_SUIT_TEXTURE));

        if (this.parentModel == null) return;
        this.visor.copyFrom(parentModel.head);
        this.belt.copyFrom(parentModel.body);
        this.rightBoot.copyFrom(parentModel.rightLeg);
        this.leftBoot.copyFrom(parentModel.leftLeg);
        parentModel.copyPropertiesTo(this);

        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.visor.visible = visible;
        this.belt.visible = visible;
        this.rightBoot.visible = visible;
        this.leftBoot.visible = visible;
    }

    private void setVisible() {
        this.setAllVisible(false);
        switch (this.slot) {
            case HEAD -> {
                this.head.visible = true;
                this.visor.visible = true;
            }
            case CHEST -> {
                this.body.visible = true;
                this.rightArm.visible = true;
                this.leftArm.visible = true;
            }
            case LEGS -> {
                this.belt.visible = true;
                this.rightLeg.visible = true;
                this.leftLeg.visible = true;
            }
            case FEET -> {
                this.rightBoot.visible = true;
                this.leftBoot.visible = true;
            }
        }
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of(head, visor);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(body, rightArm, leftArm, rightLeg, leftLeg, hat, belt, rightBoot, leftBoot);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(1.0f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.6f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild(PartNames.HAT, CubeListBuilder.create().texOffs(0, 0), PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offset(0.0f, 0.5f, 0.0f));
        root.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.5f)), PartPose.offset(0.0f, -0.5f, 0.0f));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0f, 2.0f, 0.0f));
        rightArm.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 32).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offsetAndRotation(-1.0f, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f));

        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0f, 2.0f, 0.0f));
        leftArm.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-2.0f, -6.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offsetAndRotation(1.0f, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0f, 0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.7f)), PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2.0f, 0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.7f)), PartPose.offset(1.9f, 12.0f, 0.0f));

        root.addOrReplaceChild("right_boot", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0f, 0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f))
                .texOffs(16, 48).addBox(-2.0f, 0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(1.0f)), PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_boot", CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-2.0f, 0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f))
                .texOffs(16, 48).addBox(-2.0f, 0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(1.0f)), PartPose.offset(1.9f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }
}