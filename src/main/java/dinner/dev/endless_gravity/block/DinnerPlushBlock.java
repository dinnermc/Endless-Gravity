package dinner.dev.endless_gravity.block;

import dinner.dev.endless_gravity.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

public class DinnerPlushBlock extends HorizontalFacingBlock {

    private static final int PLAY_COOLDOWN_TICKS = 10;
    private static final int EXPLOSION_HITS = 999;
    private static final Map<BlockPos, Long> LAST_PLAY = new HashMap<>();
    private static final Map<BlockPos, Integer> HITS = new HashMap<>();

    public DinnerPlushBlock(Properties properties, VoxelShape shape) {
        super(properties, shape);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        playToy(level, pos);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        playToy(level, pos);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        playToy(level, pos);
        onHit(level, pos);
    }

    private static void onHit(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        int hits = HITS.merge(pos, 1, Integer::sum);
        if (hits >= EXPLOSION_HITS) {
            HITS.remove(pos);
            level.playSound(null, pos, ModSounds.DINNER_PLUSH_TOY.get(), SoundSource.BLOCKS, 1.0F, 0.5F);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.BLOCK);
        }
    }

    private static void playToy(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        long gameTime = level.getGameTime();
        Long last = LAST_PLAY.get(pos);
        if (last != null && gameTime - last < PLAY_COOLDOWN_TICKS) {
            return;
        }
        LAST_PLAY.put(pos, gameTime);
        float pitch = 0.85F + level.random.nextFloat() * 0.3F;
        level.playSound(null, pos, ModSounds.DINNER_PLUSH_TOY.get(), SoundSource.BLOCKS, 0.5F, pitch);
    }

    @Override
    @Deprecated
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            HITS.remove(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
