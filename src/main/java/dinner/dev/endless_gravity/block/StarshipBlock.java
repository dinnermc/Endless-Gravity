package dinner.dev.endless_gravity.block;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dinner.dev.endless_gravity.client.sound.StarshipEngineSoundController;
import dinner.dev.endless_gravity.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StarshipBlock extends HorizontalFacingBlock {

    private static final Map<BlockPos, Long> LAST_LAUNCH = new HashMap<>();
    private static final Map<ServerSubLevel, RocketState> FLYING = new HashMap<>();

    private static long launchCooldownTicks() {
        return 20L;
    }

    private static double launchVelocity() {
        return 0.5;
    }

    private static double shipMass() {
        return 4.0;
    }

    private static double boosterMass() {
        return 1.0;
    }

    private static double gravity() {
        return 9.81;
    }

    private static long liftoffTicks() {
        return 10L;
    }

    private static long rampEndTicks() {
        return 30L;
    }

    private static double liftoffThrustG() {
        return 1.25;
    }

    private static double maxThrustG() {
        return 1.39;
    }

    private static double detachY() {
        return 1800.0;
    }

    private static double peakY() {
        return 2500.0;
    }

    private static long maxAscentTicks() {
        return 700L;
    }

    private static long maxCruiseTicks() {
        return 400L;
    }

    private static double maxLandingG() {
        return 6.0;
    }

    private static double settleDistance() {
        return 0.5;
    }

    private static long landingTimeoutTicks() {
        return 12000L;
    }

    private static double cruiseLateralG() {
        return 0.5;
    }

    private static double horizontalBrakeK() {
        return 2.0;
    }

    private static double driftSpeed() {
        return 30.0;
    }

    private static double driftAccel() {
        return 2.0;
    }

    private static int driftBoostTicks() {
        return 200;
    }

    private static double driftTilt() {
        return 0.22;
    }

    private static double tiltDrive() {
        return 6.0;
    }

    private static double tiltResponse() {
        return 0.2;
    }

    private static double boosterBurnDistance() {
        return 250.0;
    }

    private static final class RocketState {
        final double mass;
        final double burnDistance;
        final long launchTime;
        final BlockPos padPos;
        final ResourceKey<Level> dimension;
        boolean detached;
        boolean shutdown;
        boolean flipped;
        double lateralX;
        double lateralZ;
        int driftBoostRemaining;

        RocketState(double mass, double burnDistance, long launchTime, BlockPos padPos, ResourceKey<Level> dimension) {
            this(mass, burnDistance, launchTime, false, false, padPos, dimension);
        }

        RocketState(double mass, double burnDistance, long launchTime, boolean detached, boolean shutdown, BlockPos padPos, ResourceKey<Level> dimension) {
            this.mass = mass;
            this.burnDistance = burnDistance;
            this.launchTime = launchTime;
            this.detached = detached;
            this.shutdown = shutdown;
            this.padPos = padPos;
            this.dimension = dimension;
        }
    }

    public StarshipBlock(Properties properties, VoxelShape shape) {
        super(properties, shape);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.literal("Gateway to Mars").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!level.isClientSide && level.getBestNeighborSignal(pos) > 0) {
            launch(level, pos);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (FLYING.isEmpty()) {
            return;
        }
        List<AbstractMap.SimpleEntry<ServerSubLevel, RocketState>> pendingBoosters = new ArrayList<>();
        Iterator<Map.Entry<ServerSubLevel, RocketState>> iterator = FLYING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerSubLevel, RocketState> entry = iterator.next();
            ServerSubLevel subLevel = entry.getKey();
            RocketState state = entry.getValue();

            if (subLevel.isRemoved()) {
                iterator.remove();
                continue;
            }

            ServerLevel level = subLevel.getLevel();
            long elapsed = level.getGameTime() - state.launchTime;
            RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
            if (handle == null || !handle.isValid()) {
                iterator.remove();
                continue;
            }

            Pose3d pose = subLevel.logicalPose();
            double y = pose.position().y;
            Vec3 soundPos = new Vec3(pose.position().x, pose.position().y, pose.position().z);

            boolean engineFiring = false;
            float thrustPower = 0.0f;

            if (!state.detached) {
                double thrustG = thrustProfile(elapsed);
                if (thrustG > 0.0) {
                    Vector3d velocity = handle.getLinearVelocity(new Vector3d());
                    applyThrust(handle, subLevel, state, thrustG);
                    spawnExhaust(level, pose, velocity);
                    engineFiring = true;
                    thrustPower = (float) Math.min(1.0, thrustG / maxThrustG());
                }
                StarshipEngineSoundController.tick(state.padPos, state.dimension, soundPos, engineFiring, thrustPower);

                if (y >= detachY() || elapsed > maxAscentTicks()) {
                    ServerSubLevel booster = detach(subLevel, state);
                    if (booster != null) {
                        pendingBoosters.add(new AbstractMap.SimpleEntry<>(booster, state));
                    }
                }
            } else if (!state.shutdown) {
                if (y < peakY() && elapsed <= maxAscentTicks() + maxCruiseTicks()) {
                    applyLateralThrust(handle, subLevel, state);
                    engineFiring = state.lateralX != 0.0 || state.lateralZ != 0.0;
                    thrustPower = engineFiring ? 0.3f : 0.0f;
                } else {
                    state.shutdown = true;
                }
                StarshipEngineSoundController.tick(state.padPos, state.dimension, soundPos, engineFiring, thrustPower);
            } else {
                Vector3d velocity = handle.getLinearVelocity(new Vector3d());
                double groundY = groundY(level, pose.position());
                double distance = pose.position().y - groundY;

                boolean inDriftZone = distance >= state.burnDistance && (state.lateralX != 0.0 || state.lateralZ != 0.0);
                boolean inLandingZone = distance < state.burnDistance;

                if (velocity.y < -0.3) {
                    if (distance < settleDistance()) {
                        settle(subLevel, handle, groundY);
                        StarshipEngineSoundController.tick(state.padPos, state.dimension, soundPos, false, 0.0f);
                        iterator.remove();
                        continue;
                    }

                    if (inDriftZone) {
                        double horizontalSpeed = Math.hypot(velocity.x, velocity.z);
                        if (horizontalSpeed < driftSpeed() && state.driftBoostRemaining > 0) {
                            double thrustMass = bodyMass(subLevel, state);
                            handle.applyLinearImpulse(new Vector3d(
                                    state.lateralX * thrustMass * driftAccel() / 20.0,
                                    0.0,
                                    state.lateralZ * thrustMass * driftAccel() / 20.0));
                            state.driftBoostRemaining--;
                            spawnDriftPuffs(level, pose, state);
                        }
                    }

                    if (inLandingZone) {
                        if (!state.flipped) {
                            state.flipped = true;
                            Vector3d angular = handle.getAngularVelocity(new Vector3d());
                            handle.addLinearAndAngularVelocity(JOMLConversion.ZERO, angular.negate());
                        }
                        Vector3d thrust = new Vector3d(0.0,
                                Math.min(velocity.y * velocity.y / (2.0 * distance) + gravity(), maxLandingG() * gravity()),
                                0.0);
                        double horizontalSpeed = Math.hypot(velocity.x, velocity.z);
                        if (horizontalSpeed > 0.1) {
                            double horizontalAccel = Math.min(horizontalSpeed * horizontalBrakeK(),
                                    maxLandingG() * gravity() - thrust.y);
                            thrust.x = -velocity.x / horizontalSpeed * horizontalAccel;
                            thrust.z = -velocity.z / horizontalSpeed * horizontalAccel;
                        }
                        applyForce(handle, subLevel, state, thrust);
                        tiltToward(handle, pose, thrust, distance);
                        spawnExhaust(level, pose, velocity);
                    }
                }

                if (inDriftZone) {
                    engineFiring = true;
                    thrustPower = 0.5f;
                } else if (inLandingZone) {
                    engineFiring = true;
                    thrustPower = 0.8f;
                }

                StarshipEngineSoundController.tick(state.padPos, state.dimension, soundPos, engineFiring, thrustPower);

                if (elapsed > landingTimeoutTicks()) {
                    StarshipEngineSoundController.tick(state.padPos, state.dimension, soundPos, false, 0.0f);
                    iterator.remove();
                }
            }
        }
        for (AbstractMap.SimpleEntry<ServerSubLevel, RocketState> entry : pendingBoosters) {
            ServerSubLevel booster = entry.getKey();
            RocketState parentState = entry.getValue();
            RocketState boosterState = new RocketState(boosterMass(), boosterBurnDistance(), booster.getLevel().getGameTime(), true, true, parentState.padPos, parentState.dimension);
            boosterState.driftBoostRemaining = driftBoostTicks();
            FLYING.put(booster, boosterState);
        }
    }

    private static double thrustProfile(long elapsed) {
        if (elapsed < liftoffTicks()) {
            return liftoffThrustG();
        }
        if (elapsed < rampEndTicks()) {
            double fraction = (elapsed - liftoffTicks()) / (double) (rampEndTicks() - liftoffTicks());
            return liftoffThrustG() + (maxThrustG() - liftoffThrustG()) * fraction;
        }
        return maxThrustG();
    }

    private static double bodyMass(ServerSubLevel subLevel, RocketState state) {
        double mass = subLevel.getSelfMassTracker().getMass();
        return mass > 0.0 ? mass : state.mass;
    }

    private static void applyThrust(RigidBodyHandle handle, ServerSubLevel subLevel, RocketState state, double thrustG) {
        handle.applyLinearImpulse(new Vector3d(0.0, bodyMass(subLevel, state) * thrustG * gravity() / 20.0, 0.0));
    }

    private static void applyForce(RigidBodyHandle handle, ServerSubLevel subLevel, RocketState state, Vector3d acceleration) {
        handle.applyLinearImpulse(new Vector3d(acceleration).mul(bodyMass(subLevel, state) / 20.0));
    }

    private static void applyLateralThrust(RigidBodyHandle handle, ServerSubLevel subLevel, RocketState state) {
        if (state.lateralX == 0.0 && state.lateralZ == 0.0) {
            return;
        }
        handle.applyLinearImpulse(new Vector3d(
                state.lateralX * bodyMass(subLevel, state) * cruiseLateralG() * gravity() / 20.0,
                0.0,
                state.lateralZ * bodyMass(subLevel, state) * cruiseLateralG() * gravity() / 20.0));
    }

    private static void tiltToward(RigidBodyHandle handle, Pose3d pose, Vector3d thrust, double distance) {
        double length = thrust.length();
        if (length < 1.0e-6) {
            return;
        }
        Vector3d direction = new Vector3d(thrust).normalize();
        double uprightness = Mth.clamp(distance / 40.0, 0.0, 1.0);
        if (uprightness < 1.0) {
            direction.lerp(new Vector3d(0.0, 1.0, 0.0), 1.0 - uprightness).normalize();
        }
        Quaterniond target = new Quaterniond().rotationTo(new Vector3d(0.0, 1.0, 0.0), direction);
        Quaterniond current = new Quaterniond(pose.orientation());
        Quaterniond delta = new Quaterniond();
        current.difference(target, delta);
        double axisLength = Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
        if (axisLength < 1.0e-6) {
            return;
        }
        double angle = 2.0 * Math.atan2(axisLength, delta.w);
        Vector3d currentAngular = handle.getAngularVelocity(new Vector3d());
        Vector3d desired = new Vector3d(delta.x / axisLength, delta.y / axisLength, delta.z / axisLength)
                .mul(angle * tiltDrive());
        handle.addLinearAndAngularVelocity(JOMLConversion.ZERO, desired.sub(currentAngular).mul(tiltResponse()));
    }

    private static ServerSubLevel detach(ServerSubLevel subLevel, RocketState state) {
        state.detached = true;
        state.shutdown = true;
        state.driftBoostRemaining = driftBoostTicks();
        ServerLevel level = subLevel.getLevel();
        LevelPlot plot = subLevel.getPlot();

        BlockState current = plot.getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO);
        BlockState upper = ModBlocks.STARSHIP_UPPER.get().defaultBlockState();
        if (current.hasProperty(HorizontalFacingBlock.FACING)) {
            Direction facing = current.getValue(HorizontalFacingBlock.FACING);
            upper = upper.setValue(HorizontalFacingBlock.FACING, facing);
            state.lateralX = facing.getStepX();
            state.lateralZ = facing.getStepZ();
        }
        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, upper, 3);

        RigidBodyHandle shipHandle = RigidBodyHandle.of(subLevel);
        if (shipHandle != null && shipHandle.isValid() && (state.lateralX != 0.0 || state.lateralZ != 0.0)) {
            Vector3d nose = new Vector3d(
                    state.lateralX * Math.sin(driftTilt()),
                    Math.cos(driftTilt()),
                    state.lateralZ * Math.sin(driftTilt()));
            Pose3d pose = subLevel.logicalPose();
            shipHandle.teleport(new Vector3d(pose.position()),
                    new Quaterniond().rotationTo(new Vector3d(0.0, 1.0, 0.0), nose));
        }

        return spawnBooster(level, subLevel.logicalPose().position(), shipHandle);
    }

    private static ServerSubLevel spawnBooster(ServerLevel level, Vector3d shipPosition, RigidBodyHandle shipHandle) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        Pose3d pose = new Pose3d();
        pose.position().set(shipPosition.x, shipPosition.y - 2.0, shipPosition.z);

        ServerSubLevel booster = (ServerSubLevel) container.allocateNewSubLevel(pose);
        LevelPlot plot = booster.getPlot();
        plot.newEmptyChunk(plot.getCenterChunk());
        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, ModBlocks.SUPER_HEAVY.get().defaultBlockState(), 3);
        booster.updateLastPose();

        RigidBodyHandle handle = RigidBodyHandle.of(booster);
        if (handle != null && handle.isValid()) {
            Vector3d velocity = new Vector3d(0.0, shipHandle.getLinearVelocity(new Vector3d()).y, 0.0);
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            velocity.x = Math.cos(angle) * 1.0;
            velocity.z = Math.sin(angle) * 1.0;
            handle.addLinearAndAngularVelocity(velocity, JOMLConversion.ZERO);
        }

        return booster;
    }

    private static void settle(ServerSubLevel subLevel, RigidBodyHandle handle, double groundY) {
        Vector3d position = subLevel.logicalPose().position();
        handle.teleport(new Vector3d(position.x, groundY + 0.5, position.z), new Quaterniond());
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        Vector3d angular = handle.getAngularVelocity(new Vector3d());
        handle.addLinearAndAngularVelocity(velocity.negate(), angular.negate());
    }

    private static void spawnExhaust(ServerLevel level, Pose3d pose, Vector3d velocity) {
        RandomSource random = level.getRandom();
        double tail = 18.0 + Math.max(velocity.y, 0.0) * 1.2;
        int tailPuffs = 4;
        for (int i = 0; i < tailPuffs; i++) {
            double px = pose.position().x + (random.nextDouble() - 0.5) * 2.0;
            double py = pose.position().y - tail + (random.nextDouble() - 0.5) * 1.5;
            double pz = pose.position().z + (random.nextDouble() - 0.5) * 2.0;
            double vx = (random.nextDouble() - 0.5) * 0.05;
            double vy = -0.02 + (random.nextDouble() - 0.5) * 0.04;
            double vz = (random.nextDouble() - 0.5) * 0.05;
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            for (ServerPlayer player : level.players()) {
                level.sendParticles(player, ModParticles.TAIL_SMOKE.get(), true,
                        px, py, pz, 0, vx / speed, vy / speed, vz / speed, speed);
            }
        }

        int exhaustCount = 6;
        double drop = 1.0 + Math.min(Math.abs(velocity.y) * 0.25, 2.5);
        double backX = -velocity.x * 0.06;
        double backZ = -velocity.z * 0.06;

        for (int i = 0; i < exhaustCount; i++) {
            double ex = pose.position().x + backX + (random.nextDouble() - 0.5) * 0.24;
            double ey = pose.position().y - drop + (random.nextDouble() - 0.5) * 0.24;
            double ez = pose.position().z + backZ + (random.nextDouble() - 0.5) * 0.24;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double spread = 0.07 + random.nextDouble() * 0.08;
            double flowSpeed = 1.2 + random.nextDouble() * 0.6;
            double exv = Math.cos(angle) * spread + velocity.x * 0.35;
            double eyv = Math.min(velocity.y, 0.0) * 0.5 - flowSpeed;
            double ezv = Math.sin(angle) * spread + velocity.z * 0.35;
            double espeed = Math.sqrt(exv * exv + eyv * eyv + ezv * ezv);
            for (ServerPlayer player : level.players()) {
                level.sendParticles(player, ModParticles.EXHAUST_SMOKE.get(), true,
                        ex, ey, ez, 0, exv / espeed, eyv / espeed, ezv / espeed, espeed);
            }
        }
    }

    private static void spawnDriftPuffs(ServerLevel level, Pose3d pose, RocketState state) {
        RandomSource random = level.getRandom();
        int driftExhaust = 2;
        for (int i = 0; i < driftExhaust; i++) {
            double px = pose.position().x - state.lateralX * 0.9 + (random.nextDouble() - 0.5) * 0.5;
            double py = pose.position().y - 0.6 + (random.nextDouble() - 0.5) * 0.3;
            double pz = pose.position().z - state.lateralZ * 0.9 + (random.nextDouble() - 0.5) * 0.5;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double spread = 0.05 + random.nextDouble() * 0.05;
            double flowSpeed = 0.6 + random.nextDouble() * 0.4;
            double vx = -state.lateralX * flowSpeed + Math.cos(angle) * spread;
            double vy = -0.15 + (random.nextDouble() - 0.5) * 0.1;
            double vz = -state.lateralZ * flowSpeed + Math.sin(angle) * spread;
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            for (ServerPlayer player : level.players()) {
                level.sendParticles(player, ModParticles.EXHAUST_SMOKE.get(), true,
                        px, py, pz, 0, vx / speed, vy / speed, vz / speed, speed);
            }
        }

        if (random.nextFloat() < 0.3F) {
            double px = pose.position().x - state.lateralX * 1.2 + (random.nextDouble() - 0.5) * 0.8;
            double py = pose.position().y - 0.9 + (random.nextDouble() - 0.5) * 0.4;
            double pz = pose.position().z - state.lateralZ * 1.2 + (random.nextDouble() - 0.5) * 0.8;
            double vx = -state.lateralX * 0.15 + (random.nextDouble() - 0.5) * 0.05;
            double vy = -0.1;
            double vz = -state.lateralZ * 0.15 + (random.nextDouble() - 0.5) * 0.05;
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            for (ServerPlayer player : level.players()) {
                level.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                        px, py, pz, 0, vx / speed, vy / speed, vz / speed, speed);
            }
        }
    }

    private static double groundY(ServerLevel level, Vector3d position) {
        MutableBlockPos mutablePos = new MutableBlockPos();
        int x = Mth.floor(position.x);
        int z = Mth.floor(position.z);
        for (int y = level.getHeight(); y > level.getMinBuildHeight(); y--) {
            mutablePos.set(x, y, z);
            if (!level.getBlockState(mutablePos).isAir()) {
                return y + 1.0;
            }
        }
        return level.getMinBuildHeight();
    }

    private static void launch(Level level, BlockPos pos) {
        long gameTime = level.getGameTime();
        Long last = LAST_LAUNCH.get(pos);
        if (last != null && gameTime - last < launchCooldownTicks()) {
            return;
        }
        LAST_LAUNCH.put(pos, gameTime);

        SubLevel containing = Sable.HELPER.getContaining(level, pos);
        if (containing != null) {
            if (containing instanceof ServerSubLevel server && !FLYING.containsKey(server)) {
                RigidBodyHandle handle = RigidBodyHandle.of(server);
                if (handle != null && handle.isValid()) {
                    handle.addLinearAndAngularVelocity(
                            new Vector3d(0.0, launchVelocity(), 0.0),
                            JOMLConversion.ZERO);
                    FLYING.put(server, new RocketState(shipMass(), boosterBurnDistance(), gameTime, pos, level.dimension()));
                }
            }
            return;
        }

        Set<BlockPos> blocks = Set.of(pos);
        BoundingBox3i bounds = new BoundingBox3i(pos, pos);
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel) level, pos, blocks, bounds);

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) {
            return;
        }

        handle.addLinearAndAngularVelocity(
                new Vector3d(0.0, launchVelocity(), 0.0),
                JOMLConversion.ZERO);
        FLYING.put(subLevel, new RocketState(shipMass(), boosterBurnDistance(), gameTime, pos, level.dimension()));
    }
}
