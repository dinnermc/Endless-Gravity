package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Set;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.EXTEfx.*;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow @Final private com.mojang.blaze3d.audio.Library library;

    @Unique private static int endlessgravity$lowPassFilter = -1;
    @Unique private static boolean endlessgravity$lastFiltering = false;
    @Unique private static boolean endlessgravity$refsResolved = false;
    @Unique private static Field endlessgravity$libraryStaticChannels;
    @Unique private static Field endlessgravity$libraryStreamingChannels;
    @Unique private static Field endlessgravity$channelPoolActiveChannels;
    @Unique private static Field endlessgravity$channelSource;
    @Unique private static float endlessgravity$lastGain = -1f;
    @Unique private static float endlessgravity$lastGainHF = -1f;
    @Unique private static final Set<Channel> endlessgravity$EMPTY_SET = Set.of();

    @Unique
    private static void endlessgravity$resolveRefs() {
        if (endlessgravity$refsResolved) return;
        try {
            endlessgravity$libraryStaticChannels = com.mojang.blaze3d.audio.Library.class.getDeclaredField("staticChannels");
            endlessgravity$libraryStaticChannels.setAccessible(true);

            endlessgravity$libraryStreamingChannels = com.mojang.blaze3d.audio.Library.class.getDeclaredField("streamingChannels");
            endlessgravity$libraryStreamingChannels.setAccessible(true);

            Class<?> poolClass = Class.forName("com.mojang.blaze3d.audio.Library$CountingChannelPool");
            endlessgravity$channelPoolActiveChannels = poolClass.getDeclaredField("activeChannels");
            endlessgravity$channelPoolActiveChannels.setAccessible(true);

            endlessgravity$channelSource = Channel.class.getDeclaredField("source");
            endlessgravity$channelSource.setAccessible(true);

            endlessgravity$refsResolved = true;
        } catch (Exception e) {
            endlessgravity$refsResolved = true;
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static Set<Channel> endlessgravity$getChannels(Object pool) {
        try {
            return (Set<Channel>) endlessgravity$channelPoolActiveChannels.get(pool);
        } catch (Exception e) {
            return endlessgravity$EMPTY_SET;
        }
    }

    @Unique
    private static int endlessgravity$getSource(Channel channel) {
        try {
            return endlessgravity$channelSource.getInt(channel);
        } catch (Exception e) {
            return 0;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void endlessgravity$applyLowPass(boolean paused, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean inEnd = mc.level.dimension() == Level.END;

        boolean filterEnabled;
        float gain;
        float gainHF;
        try {
            filterEnabled = Config.COMMON.enableLowPassFilter.get();
            gain = Config.COMMON.lowPassGain.get().floatValue();
            gainHF = Config.COMMON.lowPassGainHF.get().floatValue();
        } catch (Exception e) {
            return;
        }

        boolean shouldFilter = inEnd && filterEnabled;

        endlessgravity$resolveRefs();

        if (shouldFilter && endlessgravity$lowPassFilter == -1) {
            endlessgravity$lowPassFilter = alGenFilters();
            alFilteri(endlessgravity$lowPassFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            endlessgravity$lastGain = -1f;
            endlessgravity$lastGainHF = -1f;
        }

        if (shouldFilter && endlessgravity$lowPassFilter != -1) {
            if (gain != endlessgravity$lastGain || gainHF != endlessgravity$lastGainHF) {
                alFilterf(endlessgravity$lowPassFilter, AL_LOWPASS_GAIN, gain);
                alFilterf(endlessgravity$lowPassFilter, AL_LOWPASS_GAINHF, gainHF);
                endlessgravity$lastGain = gain;
                endlessgravity$lastGainHF = gainHF;
            }
        }

        int filterValue = shouldFilter ? endlessgravity$lowPassFilter : AL_FILTER_NULL;

        try {
            Object staticPool = endlessgravity$libraryStaticChannels.get(this.library);
            Object streamingPool = endlessgravity$libraryStreamingChannels.get(this.library);

            for (int i = 0; i < 2; i++) {
                Object pool = i == 0 ? staticPool : streamingPool;
                if (pool == null) continue;
                for (Channel channel : endlessgravity$getChannels(pool)) {
                    int sourceId = endlessgravity$getSource(channel);
                    if (sourceId == 0) continue;
                    alSourcei(sourceId, AL_DIRECT_FILTER, filterValue);
                }
            }
        } catch (Exception ignored) {
        }

        if (!shouldFilter && endlessgravity$lastFiltering && endlessgravity$lowPassFilter != -1) {
            alDeleteFilters(endlessgravity$lowPassFilter);
            endlessgravity$lowPassFilter = -1;
            endlessgravity$lastGain = -1f;
            endlessgravity$lastGainHF = -1f;
        }

        endlessgravity$lastFiltering = shouldFilter;
    }
}
