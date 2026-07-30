package dinner.dev.endless_gravity.mixin.sable;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.ServerSubLevel", remap = false)
public abstract class ServerSubLevelMixin {

    private static final double MC_GRAVITY = 0.08;

    @Shadow
    public abstract ServerLevel getLevel();

    @Redirect(
        method = "prePhysicsTick",
        at = @At(value = "INVOKE", target = "Lorg/joml/Vector3d;fma(DLorg/joml/Vector3dc;)Lorg/joml/Vector3d;")
    )
    private Vector3d eg$atmosphereGravity(Vector3d instance, double a, Vector3dc b) {
        instance.fma(a, b);

        if (Config.COMMON.enableAtmosphere.get()) {
            if (getLevel().dimension() == Level.OVERWORLD) {
                double altitudeY = ((SubLevelAccess) this).logicalPose().position().y();
                double offset = EndlessGravityAPI.getAtmosphereOffset(altitudeY);
                if (offset > 0) {
                    double counterVelocity = a * b.y() * (offset / MC_GRAVITY);
                    instance.add(0, counterVelocity, 0);
                }
            }
        }

        return instance;
    }
}
