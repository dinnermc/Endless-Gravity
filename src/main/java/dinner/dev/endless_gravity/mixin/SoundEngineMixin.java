package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.MuffleChannelAccess;
import dinner.dev.endless_gravity.MuffleState;
import dinner.dev.endless_gravity.api.AudioMuffleContext;
import dinner.dev.endless_gravity.api.AudioMuffleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow @Final private SoundEngineExecutor executor;
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void endlessgravity$applyLowPass(boolean paused, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.executor.execute(MuffleState::resetAudioThread);
            return;
        }

        if (!Config.COMMON.enableLowPassFilter.get()) {
            this.executor.execute(() -> MuffleState.tickAudioThread(1.0f, 1.0f));
            return;
        }

        double playerY = EndlessGravityAPI.getRealY(mc.player);

        boolean inEnd = mc.level.dimension() == Level.END;
        Config.CustomDimConfig dimensionConfig = Config.getCustomDimConfig(mc.level.dimension().location()).orElse(null);
        boolean atmosphereEnabled = dimensionConfig != null && dimensionConfig.isEnableAtmosphere()
                && (!EndlessGravityAPI.isOverworld(mc.level)
                || (Config.COMMON.enableAtmosphere.get() && Config.COMMON.overworldEntityGravity.get()));
        double atmosphereProgress = atmosphereEnabled ? dimensionConfig.getAtmosphereProgress(playerY) : 0.0;
        boolean atmFiltering = atmosphereEnabled && atmosphereProgress > 0.0;

        float targetGain = 1.0f;
        float targetGainHF = 1.0f;

        if (dimensionConfig != null && dimensionConfig.getMode() == Config.CustomDimConfig.Mode.FIXED_GRAVITY
                && dimensionConfig.hasFixedAudioMuffle()) {
            targetGain = Math.min(targetGain, (float) dimensionConfig.getFixedMuffleGain());
            targetGainHF = Math.min(targetGainHF, (float) dimensionConfig.getFixedMuffleGainHF());
        } else if (inEnd) {
            targetGain = Math.min(targetGain, Config.COMMON.lowPassGain.get().floatValue());
            targetGainHF = Math.min(targetGainHF, Config.COMMON.lowPassGainHF.get().floatValue());
        }

        if (atmFiltering) {
            float atmGain = (float) dimensionConfig.getAtmosphereMuffleGain(playerY);
            float atmGainHF = (float) dimensionConfig.getAtmosphereMuffleGainHF(playerY);
            targetGain = Math.min(targetGain, atmGain);
            targetGainHF = Math.min(targetGainHF, atmGainHF);
        }

        if (AudioMuffleRegistry.hasModifiers()) {
            AudioMuffleContext muffleCtx = new AudioMuffleContext(mc.level, mc.player, playerY, targetGain, targetGainHF);
            AudioMuffleRegistry.apply(muffleCtx);
            targetGain = muffleCtx.effectiveGain();
            targetGainHF = muffleCtx.effectiveGainHF();
        }

        final float g = targetGain;
        final float ghf = targetGainHF;
        this.executor.execute(() -> MuffleState.tickAudioThread(g, ghf));
    }

    @Inject(method = "play", at = @At("TAIL"))
    private void endlessgravity$classifyPlayedSound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null) return;
        ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(sound);
        if (handle == null) return;
        MuffleState.ChannelRole role = endlessgravity$classifyRole(sound);
        handle.execute(channel -> {
            if (channel instanceof MuffleChannelAccess access) {
                MuffleState.classifySource(access.endlessgravity$getSourceId(), role);
            }
        });
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void endlessgravity$onDestroy(CallbackInfo ci) {
        MuffleState.onAudioContextDestroyed();
    }

    @Unique
    private static MuffleState.ChannelRole endlessgravity$classifyRole(SoundInstance sound) {
        SoundSource category = sound.getSource();
        if (category == SoundSource.MUSIC) {
            return MuffleState.ChannelRole.EXEMPT;
        }
        if (category == SoundSource.MASTER
                && (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE)) {
            return MuffleState.ChannelRole.EXEMPT;
        }
        if (category == SoundSource.PLAYERS) {
            if (sound.isRelative()) {
                return MuffleState.ChannelRole.SELF_SUIT;
            }
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                double dx = sound.getX() - player.getX();
                double dy = sound.getY() - player.getY();
                double dz = sound.getZ() - player.getZ();
                if (dx * dx + dy * dy + dz * dz <= 9.0) {
                    return MuffleState.ChannelRole.SELF_SUIT;
                }
            }
        }
        return MuffleState.ChannelRole.WORLD;
    }
}
