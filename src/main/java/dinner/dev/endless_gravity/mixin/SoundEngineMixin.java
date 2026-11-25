package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.AtmosphereLayers;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.CustomDimConfig;
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
        CustomDimConfig profile = Config.getCustomDimConfig(mc.level.dimension().location()).orElse(null);
        if (profile == null && EndlessGravityAPI.isSableManaged(mc.level)) {
            CustomDimConfig overworldProfile = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
            if (overworldProfile != null && overworldProfile.getMode() == CustomDimConfig.Mode.ATMOSPHERE) {
                profile = overworldProfile;
            }
        }
        boolean profileAtmosphere = profile != null && profile.getMode() == CustomDimConfig.Mode.ATMOSPHERE
                && (!EndlessGravityAPI.isSableManaged(mc.level) || Config.COMMON.enableAtmosphere.get())
                && !(EndlessGravityAPI.isOverworldOrSable(mc.level)
                && EndlessGravityAPI.isCosmonauticsInstalled());
        boolean globalAtmosphere = !profileAtmosphere && EndlessGravityAPI.isSableManaged(mc.level)
                && !EndlessGravityAPI.isCosmonauticsInstalled() && Config.COMMON.enableAtmosphere.get();
        double audioMuffleProgress = profileAtmosphere
                ? profile.getAtmosphereMuffleProgress(playerY) : AtmosphereLayers.getAudioMuffleProgress(playerY);
        boolean atmFiltering = (profileAtmosphere || globalAtmosphere) && audioMuffleProgress > 0.0;

        float targetGain = 1.0f;
        float targetGainHF = 1.0f;

        if (profile != null && profile.getMode() == CustomDimConfig.Mode.FIXED_GRAVITY && profile.hasFixedAudioMuffle()) {
            targetGain = Math.min(targetGain, (float) profile.getFixedMuffleGain());
            targetGainHF = Math.min(targetGainHF, (float) profile.getFixedMuffleGainHF());
        } else if (inEnd) {
            targetGain = Math.min(targetGain, Config.COMMON.lowPassGain.get().floatValue());
            targetGainHF = Math.min(targetGainHF, Config.COMMON.lowPassGainHF.get().floatValue());
        }

        if (atmFiltering) {
            float atmGain = (float) (profileAtmosphere
                    ? profile.getAtmosphereMuffleGain(playerY)
                    : EndlessGravityAPI.getAtmosphereMuffleGain(playerY));
            float atmGainHF = (float) (profileAtmosphere
                    ? profile.getAtmosphereMuffleGainHF(playerY)
                    : EndlessGravityAPI.getAtmosphereMuffleGainHF(playerY));
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
