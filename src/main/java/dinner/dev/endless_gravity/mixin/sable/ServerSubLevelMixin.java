package dinner.dev.endless_gravity.mixin.sable;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dinner.dev.endless_gravity.AtmosphereLayers;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.CustomDimConfig;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityGameRules;
import dinner.dev.endless_gravity.compat.sable.AtmosphereForceGroups;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerSubLevel.class, remap = false)
public abstract class ServerSubLevelMixin {

    @Unique private static final Logger endlessgravity$LOGGER = LogUtils.getLogger();
    @Unique private static final Vector3d endlessgravity$WORLD_UP = new Vector3d(0.0, 1.0, 0.0);
    /** Tuned reference gravity scale for Overworld Sable lift (decoupled from The End's sableGravityY). */
    @Unique private static final double endlessgravity$OVERWORLD_SABLE_GRAVITY = 4.0;
    @Unique private static final double endlessgravity$MAX_FORCE_MAGNITUDE = 1e6;
    @Unique private static boolean endlessgravity$forceErrorLogged = false;

    @Shadow public abstract ServerLevel getLevel();
    @Shadow public abstract MassData getMassTracker();
    @Shadow public abstract QueuedForceGroup getOrCreateQueuedForceGroup(ForceGroup group);

    @Inject(
        method = "prePhysicsTick(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;D)V",
        at = @At("TAIL")
    )
    private void eg$applySpaceGravityReduction(SubLevelPhysicsSystem physicsSystem, RigidBodyHandle handle, double timeStep, CallbackInfo ci) {
        if (!Config.COMMON.enableAtmosphere.get()) return;
        if (!Config.COMMON.overworldSableGravity.get()) return;
        // Cosmonautics owns gravity in its own sub-levels; don't stack our lift on top.
        if (EndlessGravityAPI.isCosmonauticsInstalled()) return;

        ServerLevel hostLevel = this.getLevel();
        if (hostLevel == null || hostLevel.dimension() != Level.OVERWORLD) return;
        if (!GravityGameRules.enabled(hostLevel)) return;

        try {
            SubLevelAccess subLevelAccess = (SubLevelAccess) this;
            var pose = subLevelAccess.logicalPose();
            double altitudeY = pose.position().y();
            CustomDimConfig overworldProfile = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
            boolean usesOverworldProfile = overworldProfile != null
                    && overworldProfile.getMode() == CustomDimConfig.Mode.ATMOSPHERE;
            double spaceProgress = usesOverworldProfile
                    ? overworldProfile.getAtmosphereGravityProgress(altitudeY)
                    : AtmosphereLayers.getGravityProgress(altitudeY);
            if (spaceProgress <= 0.0) return;

            MassData massData = this.getMassTracker();
            if (massData == null) return;

            double mass = massData.getMass();
            if (mass <= 0.0 || mass > 1e9 || !Double.isFinite(mass)) return;

            Vector3dc centerOfMass = massData.getCenterOfMass();
            if (centerOfMass == null) return;

            double gravityCompensation = AtmosphereLayers.getSableGravityCompensationFraction(
                    usesOverworldProfile ? overworldProfile.getAtmosphereGravityMax()
                            : Config.COMMON.atmosphereGravityMax.get());
            double levitationAccel = endlessgravity$OVERWORLD_SABLE_GRAVITY * spaceProgress * gravityCompensation;
            if (!Double.isFinite(levitationAccel) || levitationAccel <= 0.0
                    || levitationAccel > endlessgravity$OVERWORLD_SABLE_GRAVITY) return;

            double forceMagnitude = Math.min(mass * levitationAccel, endlessgravity$MAX_FORCE_MAGNITUDE);
            if (!Double.isFinite(forceMagnitude) || forceMagnitude <= 0.0) return;

            // Apply along world-up in sub-level space so rotation doesn't skew the direction.
            Vector3d forceVector = pose.transformNormalInverse(endlessgravity$WORLD_UP, new Vector3d())
                    .mul(forceMagnitude);
            if (!forceVector.isFinite()) return;

            ForceGroup spaceForceGroup = AtmosphereForceGroups.GRAVITY_REDUCTION.get();
            if (spaceForceGroup != null) {
                QueuedForceGroup queuedForceGroup = this.getOrCreateQueuedForceGroup(spaceForceGroup);
                if (queuedForceGroup != null) {
                    queuedForceGroup.applyAndRecordPointForce(centerOfMass, forceVector);
                }
            }
        } catch (Exception e) {
            if (!endlessgravity$forceErrorLogged) {
                endlessgravity$forceErrorLogged = true;
                endlessgravity$LOGGER.error("Error applying space gravity reduction to Sable sub-level", e);
            }
        }
    }
}
