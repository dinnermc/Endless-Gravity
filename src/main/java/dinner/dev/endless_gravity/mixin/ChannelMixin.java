package dinner.dev.endless_gravity.mixin;

import com.mojang.blaze3d.audio.Channel;
import dinner.dev.endless_gravity.MuffleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.EXTEfx.*;

@Mixin(Channel.class)
public abstract class ChannelMixin {

    @Shadow private int source;

    @Inject(method = "play", at = @At("HEAD"))
    private void endlessgravity$applyFilterOnPlay(CallbackInfo ci) {
        if (source == 0) return;
        alSourcei(source, AL_DIRECT_FILTER, MuffleState.getFilter());
    }

    @Inject(method = "resume", at = @At("HEAD"))
    private void endlessgravity$applyFilterOnResume(CallbackInfo ci) {
        if (source == 0) return;
        alSourcei(source, AL_DIRECT_FILTER, MuffleState.getFilter());
    }
}
