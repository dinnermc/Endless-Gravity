package dinner.dev.endless_gravity.mixin;

import com.mojang.blaze3d.audio.Channel;
import dinner.dev.endless_gravity.MuffleChannelAccess;
import dinner.dev.endless_gravity.MuffleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin implements MuffleChannelAccess {

    @Shadow private int source;

    @Override
    public int endlessgravity$getSourceId() {
        return this.source;
    }

    @Inject(method = "play", at = @At("HEAD"))
    private void endlessgravity$applyFilterOnPlay(CallbackInfo ci) {
        MuffleState.onChannelPlay(this.source);
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void endlessgravity$unregisterOnStop(CallbackInfo ci) {
        MuffleState.unregisterSource(this.source);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void endlessgravity$unregisterOnDestroy(CallbackInfo ci) {
        MuffleState.unregisterSource(this.source);
    }
}
