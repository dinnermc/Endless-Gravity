package dinner.dev.endless_gravity.mixin.sable;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.compat.sable.AtmosphereForceGroups;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.ServerSubLevel", remap = false)
public abstract class ServerSubLevelMixin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Vector3d WORLD_UP = new Vector3d(0.0, 1.0, 0.0);

    // Cached reflection references
    private static Method getMassTrackerMethod;
    private static Method getOrCreateQueuedForceGroupMethod;
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;
    private static boolean forceErrorLogged = false;

    private static void initReflection(Class<?> targetClass) {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            getMassTrackerMethod = targetClass.getMethod("getMassTracker");
            getOrCreateQueuedForceGroupMethod = targetClass.getMethod("getOrCreateQueuedForceGroup", ForceGroup.class);
        } catch (Exception e) {
            reflectionFailed = true;
            LOGGER.error("Failed to initialize reflection for Sable ServerSubLevelMixin", e);
        }
    }

    @Inject(
        method = "prePhysicsTick(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;D)V",
        at = @At("TAIL")
    )
    private void eg$applySpaceGravityReduction(SubLevelPhysicsSystem physicsSystem, RigidBodyHandle handle, double timeStep, CallbackInfo ci) {
        if (!Config.COMMON.enableAtmosphere.get()) return;
        if (!Config.COMMON.overworldSableGravity.get()) return;
        // Create: Cosmonautics manages gravity in its own sub-levels; don't
        // add the atmosphere levitation force on top of it.
        if (EndlessGravityAPI.isCosmonauticsInstalled()) return;

        initReflection(this.getClass());
        if (reflectionFailed) return;

        try {
            // Get sub-level's global Y position
            SubLevelAccess subLevelAccess = (SubLevelAccess) this;
            double altitudeY = subLevelAccess.logicalPose().position().y();
            
            // Get space progress: 0.0 at full atmosphere pressure -> 1.0 in vacuum,
            // following the configured atmosphere layers
            double spaceProgress = EndlessGravityAPI.getAtmosphereProgress(altitudeY);
            if (spaceProgress <= 0) return;

            // Get MassData
            MassData massData = (MassData) getMassTrackerMethod.invoke(this);
            if (massData == null) return;

            double mass = massData.getMass();
            if (mass <= 0 || mass > 1e9 || Double.isNaN(mass) || Double.isInfinite(mass)) return;

            // Use the body's center of mass as the force application point
            // This ensures zero torque (force applied at center of mass)
            // The applyImpulseAtPoint method calculates torque as (position - centerOfMass) × force
            // So we must pass the actual center of mass to avoid creating unwanted torque
            Vector3dc centerOfMass = massData.getCenterOfMass();
            if (centerOfMass == null) return;

// Levitation force: counteract Sable's gravity based on atmosphere progress
            // Sable's gravity is configurable via sableGravityY (default: -4.0). We apply upward force to counteract it.
            // The force scales with atmosphere progress: 0 at surface, partial counteraction in deep space.
            // Linear progress with 0.067 scaling so force never exceeds 6.7% of gravity (always downward net force).
            double sableGravity = Math.abs(Config.COMMON.sableGravityY.get()); 
            double levitationAccel = sableGravity * spaceProgress * 0.067;
            
            // Safety check
            if (Double.isNaN(levitationAccel) || Double.isInfinite(levitationAccel) || levitationAccel > sableGravity) return;

// Force = mass * acceleration
            double forceMagnitude = mass * levitationAccel;
            
            // Transform world-up (0, 1, 0) to sub-level local coordinates so force always opposes
            // world gravity regardless of sub-level rotation
            var pose = subLevelAccess.logicalPose();
            Vector3d localUp = pose.transformNormalInverse(WORLD_UP, new Vector3d());
            
            Vector3d forceVector = new Vector3d(
                localUp.x * forceMagnitude,
                localUp.y * forceMagnitude,
                localUp.z * forceMagnitude
            );
            
            // Validate force
            if (Double.isNaN(forceVector.y) || Double.isInfinite(forceVector.y) || Math.abs(forceVector.y) > 1e6) return;

            // Record force in Sable's force group for proper physics integration
            ForceGroup spaceForceGroup = AtmosphereForceGroups.GRAVITY_REDUCTION.get();
            if (spaceForceGroup != null) {
                QueuedForceGroup queuedForceGroup = (QueuedForceGroup) getOrCreateQueuedForceGroupMethod.invoke(this, spaceForceGroup);
                if (queuedForceGroup != null) {
                    queuedForceGroup.applyAndRecordPointForce(centerOfMass, forceVector);
                }
            }
        } catch (Exception e) {
            if (!forceErrorLogged) {
                forceErrorLogged = true;
                LOGGER.error("Error applying space gravity reduction to Sable sub-level", e);
            }
        }
    }
}