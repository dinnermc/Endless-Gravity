package dinner.dev.endless_gravity.client.sound;

import dev.ryanhcode.sable.Sable;
import dinner.dev.endless_gravity.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class StarshipEngineSoundController {

    private static final int FADE_TICKS = 15;
    private static final int MAX_AGE_TICKS = 18000;
    private static final float MIN_VOLUME = 0.35f;
    private static final float VOLUME_RANGE = 0.65f;

    private static final Map<String, EngineSound> ACTIVE = new HashMap<>();

    private StarshipEngineSoundController() {}

    public static void tick(BlockPos padPos, ResourceKey<Level> dimension, Vec3 logicalPos, boolean engineRunning, float thrustPower) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getSoundManager() == null) return;
        if (mc.level == null || !mc.level.dimension().equals(dimension)) return;

        String key = soundKey(padPos, dimension);
        EngineSound sound = ACTIVE.get(key);

        if (engineRunning) {
            if (sound == null) {
                sound = new EngineSound(dimension, logicalPos, thrustPower);
                ACTIVE.put(key, sound);
                sound.start(mc.getSoundManager());
            } else {
                sound.update(mc.getSoundManager(), logicalPos, thrustPower);
            }
        } else if (sound != null) {
            ACTIVE.remove(key);
            sound.stop(mc.getSoundManager());
        }
    }

    private static String soundKey(BlockPos padPos, ResourceKey<Level> dimension) {
        return dimension.location() + "|" + padPos.asLong();
    }

    private static final class EngineSound {
        private final ResourceKey<Level> dimension;
        private Vec3 logicalPos;
        private float thrustPower;
        private MovingEngineLoop loop;

        private EngineSound(ResourceKey<Level> dimension, Vec3 logicalPos, float thrustPower) {
            this.dimension = dimension;
            this.logicalPos = logicalPos;
            this.thrustPower = thrustPower;
        }

        private void start(SoundManager manager) {
            playOneShot(manager, dimension, logicalPos, ModSounds.STARSHIP_STARTUP.get());
            startLoop(manager);
        }

        private void update(SoundManager manager, Vec3 logicalPos, float thrustPower) {
            this.logicalPos = logicalPos;
            this.thrustPower = thrustPower;
            if (loop == null || loop.isStopped()) {
                startLoop(manager);
            } else {
                loop.updatePosition(logicalPos, thrustPower);
            }
        }

        private void startLoop(SoundManager manager) {
            loop = new MovingEngineLoop(dimension, logicalPos, thrustPower);
            manager.play(loop);
        }

        private void stop(SoundManager manager) {
            if (loop != null) {
                loop.beginFadeOut();
            }
            playOneShot(manager, dimension, logicalPos, ModSounds.STARSHIP_SHUTDOWN.get());
        }
    }

    private static void playOneShot(SoundManager manager, ResourceKey<Level> dimension, Vec3 logicalPos, SoundEvent event) {
        if (event == null) return;
        manager.play(new OneShotSound(event, dimension, logicalPos));
    }

    private static final class OneShotSound extends AbstractSoundInstance {
        private OneShotSound(SoundEvent event, ResourceKey<Level> dimension, Vec3 logicalPos) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.looping = false;
            this.delay = 0;
            this.attenuation = Attenuation.LINEAR;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null && mc.level.dimension().equals(dimension)) {
                Vec3 projected = Sable.HELPER.projectOutOfSubLevel(mc.level, logicalPos);
                this.x = projected.x;
                this.y = projected.y;
                this.z = projected.z;
            }
        }
    }

    private static final class MovingEngineLoop extends AbstractTickableSoundInstance {
        private final ResourceKey<Level> dimension;
        private Vec3 logicalPos;
        private float thrustPower;
        private int age;
        private int fadeOutRemaining;

        private MovingEngineLoop(ResourceKey<Level> dimension, Vec3 logicalPos, float thrustPower) {
            super(ModSounds.STARSHIP_LOOP.get(), SoundSource.BLOCKS, RandomSource.create());
            this.dimension = dimension;
            this.logicalPos = logicalPos;
            this.thrustPower = thrustPower;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            updateFromPad();
        }

        private void beginFadeOut() {
            fadeOutRemaining = Math.max(1, FADE_TICKS);
        }

        @Override
        public void tick() {
            if (fadeOutRemaining > 0) {
                fadeOutRemaining--;
                if (fadeOutRemaining <= 0) {
                    stop();
                    return;
                }
            }
            age++;
            if (age > MAX_AGE_TICKS) {
                beginFadeOut();
                return;
            }
            updateFromPad();
        }

        private void updateFromPad() {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || !mc.level.dimension().equals(dimension)) {
                stop();
                return;
            }
            Vec3 projected = Sable.HELPER.projectOutOfSubLevel(mc.level, logicalPos);
            this.x = projected.x;
            this.y = projected.y;
            this.z = projected.z;
            this.volume = targetVolume();
            this.pitch = 1.0f;
        }

        private void updatePosition(Vec3 logicalPos, float thrustPower) {
            this.logicalPos = logicalPos;
            this.thrustPower = thrustPower;
        }

        private float targetVolume() {
            float envelope = Math.min(1.0f, age / (float) FADE_TICKS);
            if (fadeOutRemaining > 0) {
                envelope = Math.min(envelope, fadeOutRemaining / (float) FADE_TICKS);
            }
            return (MIN_VOLUME + VOLUME_RANGE * Math.min(1.0f, Math.max(0.0f, thrustPower))) * envelope;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
