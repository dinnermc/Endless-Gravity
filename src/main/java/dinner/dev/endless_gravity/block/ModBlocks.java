package dinner.dev.endless_gravity.block;

import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, EndlessGravity.MODID);

    public static final VoxelShape DINNER_PLUSH_SHAPE = Shapes.box(
            2.8 / 16.0, 0.0, 3.7 / 16.0,
            12.8 / 16.0, 15.0 / 16.0, 12.5 / 16.0);

    public static final VoxelShape STARSHIP_SHAPE = Shapes.box(
            7.0 / 16.0, 0.0, 7.0 / 16.0,
            9.0 / 16.0, 24.0 / 16.0, 9.0 / 16.0);

    public static final VoxelShape SUPER_HEAVY_SHAPE = Shapes.box(
            7.0 / 16.0, 0.0, 7.0 / 16.0,
            9.0 / 16.0, 13.0 / 16.0, 9.0 / 16.0);

    public static final VoxelShape STARSHIP_UPPER_SHAPE = Shapes.box(
            7.0 / 16.0, 0.0, 7.0 / 16.0,
            9.0 / 16.0, 11.0 / 16.0, 9.0 / 16.0);

    public static final DeferredHolder<Block, Block> DINNER_PLUSH =
            BLOCKS.register("dinner_plush", () -> new DinnerPlushBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.5F)
                            .sound(SoundType.WOOL),
                    DINNER_PLUSH_SHAPE));

    public static final DeferredHolder<Block, Block> STARSHIP =
            BLOCKS.register("starship", () -> new StarshipBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.METAL),
                    STARSHIP_SHAPE));

    public static final DeferredHolder<Block, Block> SUPER_HEAVY =
            BLOCKS.register("super_heavy", () -> new HorizontalFacingBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.METAL),
                    SUPER_HEAVY_SHAPE));

    public static final DeferredHolder<Block, Block> STARSHIP_UPPER =
            BLOCKS.register("starship_upper", () -> new HorizontalFacingBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.METAL),
                    STARSHIP_UPPER_SHAPE));

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
