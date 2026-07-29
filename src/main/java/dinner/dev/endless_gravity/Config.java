package dinner.dev.endless_gravity;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {
        // Gravity toggles
        public final ModConfigSpec.BooleanValue enablePlayerGravity;
        public final ModConfigSpec.BooleanValue enableItemGravity;
        public final ModConfigSpec.BooleanValue enableArrowGravity;
        public final ModConfigSpec.BooleanValue enableThrownGravity;

        // Gravity values
        public final ModConfigSpec.DoubleValue playerGravityOffset;
        public final ModConfigSpec.DoubleValue itemGravityOffset;
        public final ModConfigSpec.DoubleValue arrowGravityOffset;
        public final ModConfigSpec.DoubleValue thrownGravityOffset;

        // Effects toggles
        public final ModConfigSpec.BooleanValue enableParticles;
        public final ModConfigSpec.BooleanValue enableLowPassFilter;

        // Effects values
        public final ModConfigSpec.DoubleValue particleGravityMultiplier;
        public final ModConfigSpec.DoubleValue lowPassGain;
        public final ModConfigSpec.DoubleValue lowPassGainHF;

        // Gameplay
        public final ModConfigSpec.IntValue fallDamageMode;
        public final ModConfigSpec.DoubleValue fallDamageVelocityScale;
        public final ModConfigSpec.DoubleValue fallDamageMinVelocity;
        public final ModConfigSpec.BooleanValue enableBlockGravity;
        public final ModConfigSpec.DoubleValue blockGravityOffset;

        // Sable
        public final ModConfigSpec.DoubleValue sableGravityY;
        public final ModConfigSpec.DoubleValue sablePressure;
        public final ModConfigSpec.DoubleValue sableDrag;
        public final ModConfigSpec.IntValue sableDatapackPriority;

        // Atmosphere
        public final ModConfigSpec.BooleanValue enableAtmosphere;
        public final ModConfigSpec.DoubleValue atmosphereGravityMax;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGain;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGainHF;
        public final ModConfigSpec.DoubleValue atmosphereDrag;

        // Atmosphere environment
        public final ModConfigSpec.BooleanValue enableTemperature;
        public final ModConfigSpec.IntValue temperatureFreezeInterval;
        public final ModConfigSpec.BooleanValue enableOxygen;
        public final ModConfigSpec.IntValue oxygenRate;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("gravity");

            enablePlayerGravity = builder
                    .comment("Enable player gravity reduction in The End (default: true).")
                    .define("enablePlayerGravity", true);
            playerGravityOffset = builder
                    .comment("Upward force per tick for players in The End (default: 0.055). Higher = less gravity.")
                    .defineInRange("playerGravityOffset", 0.055, 0.0, 0.07);

            enableItemGravity = builder
                    .comment("Enable item gravity reduction in The End (default: true).")
                    .define("enableItemGravity", true);
            itemGravityOffset = builder
                    .comment("Upward force per tick for items in The End (default: 0.025). Higher = less gravity.")
                    .defineInRange("itemGravityOffset", 0.025, 0.0, 0.035);

            enableArrowGravity = builder
                    .comment("Enable arrow/trident gravity reduction in The End (default: true).")
                    .define("enableArrowGravity", true);
            arrowGravityOffset = builder
                    .comment("Upward force per tick for arrows/tridents in The End (default: 0.03). Higher = less gravity.")
                    .defineInRange("arrowGravityOffset", 0.03, 0.0, 0.04);

            enableThrownGravity = builder
                    .comment("Enable thrown projectile gravity reduction in The End (default: true).")
                    .define("enableThrownGravity", true);
            thrownGravityOffset = builder
                    .comment("Upward force per tick for thrown projectiles in The End (default: 0.018). Higher = less gravity.")
                    .defineInRange("thrownGravityOffset", 0.018, 0.0, 0.025);

            builder.pop();

            builder.push("effects");

            enableParticles = builder
                    .comment("Enable particle gravity reduction in The End (default: true).")
                    .define("enableParticles", true);
            particleGravityMultiplier = builder
                    .comment("Particle gravity multiplier in The End (default: 0.3). 0 = no gravity, 1 = vanilla.")
                    .defineInRange("particleGravityMultiplier", 0.3, 0.0, 1.0);

            enableLowPassFilter = builder
                    .comment("Enable low-pass audio filter in The End (default: true). Creates a muffled underwater-like sound.")
                    .define("enableLowPassFilter", true);
            lowPassGain = builder
                    .comment("Low-pass filter volume (default: 0.1). Lower = more muffled.")
                    .defineInRange("lowPassGain", 0.1, 0.0, 1.0);
            lowPassGainHF = builder
                    .comment("Low-pass filter high-frequency volume (default: 0.05). Lower = less high-frequency sound.")
                    .defineInRange("lowPassGainHF", 0.05, 0.0, 1.0);

            builder.pop();

            builder.push("gameplay");

            fallDamageMode = builder
                    .comment("Fall damage mode in The End: 0 = normal, 1 = disabled, 2 = velocity-based (default: 2).")
                    .defineInRange("fallDamageMode", 2, 0, 2);
            fallDamageVelocityScale = builder
                    .comment("Damage per unit of velocity for velocity-based fall damage (default: 1.0). Higher = more damage.")
                    .defineInRange("fallDamageVelocityScale", 1.0, 0.1, 10.0);
            fallDamageMinVelocity = builder
                    .comment("Minimum impact velocity before velocity-based damage applies (default: 0.6). Below this, no damage.")
                    .defineInRange("fallDamageMinVelocity", 0.6, 0.0, 5.0);

            enableBlockGravity = builder
                    .comment("Enable falling block gravity reduction in The End (default: true). Affects sand, gravel, anvils, dragon eggs.")
                    .define("enableBlockGravity", true);
            blockGravityOffset = builder
                    .comment("Upward force per tick for falling blocks in The End (default: 0.035). Higher = slower fall.")
                    .defineInRange("blockGravityOffset", 0.035, 0.0, 0.035);

            builder.pop();

            builder.push("sable");

            sableGravityY = builder
                    .comment("Sable gravity Y value for The End (default: -4.0). More negative = stronger downward pull.")
                    .defineInRange("sableGravityY", -4.0, -20.0, 0.0);
            sablePressure = builder
                    .comment("Sable pressure value for The End (default: 0.0). 0 = no pressure.")
                    .defineInRange("sablePressure", 0.0, 0.0, 10.0);
            sableDrag = builder
                    .comment("Sable drag value for The End (default: 0.05). Air resistance. 0 = no drag.")
                    .defineInRange("sableDrag", 0.05, 0.0, 10.0);
            sableDatapackPriority = builder
                    .comment("Sable datapack priority (default: 9999). Must be > 1000 to override Sable built-in defaults.")
                    .defineInRange("sableDatapackPriority", 9999, 1, 9999);

            builder.pop();

            builder.push("atmosphere");

            enableAtmosphere = builder
                    .comment("Enable atmospheric gravity in the Overworld (default: true). Gravity, muffled audio, drag, temperature and oxygen scale with real atmospheric layers from Y=64 (BASE) to Y=3500 (deep space). Also affects Sable sub-levels.")
                    .define("enableAtmosphere", true);
            atmosphereGravityMax = builder
                    .comment("Maximum upward force per tick at deep space (default: 0.07). Interpolated from 0.0 at BASE Y=64. Higher = less gravity.")
                    .defineInRange("atmosphereGravityMax", 0.07, 0.0, 0.07);
            atmosphereMuffleGain = builder
                    .comment("Low-pass filter gain at deep space Y=3500 (default: 0.01). Lower = more muffled. Interpolated from 1.0 at BASE.")
                    .defineInRange("atmosphereMuffleGain", 0.01, 0.0, 1.0);
            atmosphereMuffleGainHF = builder
                    .comment("Low-pass filter high-frequency gain at deep space (default: 0.005). Lower = less high-frequency sound.")
                    .defineInRange("atmosphereMuffleGainHF", 0.005, 0.0, 1.0);
            atmosphereDrag = builder
                    .comment("Horizontal drag compensation at deep space (default: 0.03). Counteracts natural air drag: 0 = no compensation (normal drag), 0.03 = 3%% velocity retention bonus per tick at space. Interpolated from 0 at BASE.")
                    .defineInRange("atmosphereDrag", 0.03, 0.0, 0.1);

            builder.pop();

            builder.push("environment");

            enableTemperature = builder
                    .comment("Enable freezing damage at high altitude in the Overworld (default: true). Survival players freeze like in powder snow.")
                    .define("enableTemperature", true);
            temperatureFreezeInterval = builder
                    .comment("Ticks between freezing damage ticks at max altitude (default: 20). Higher = slower freezing. Scales with atmosphere progress.")
                    .defineInRange("temperatureFreezeInterval", 20, 1, 200);

            enableOxygen = builder
                    .comment("Enable oxygen depletion at high altitude in the Overworld (default: true). Survival players suffocate above the layers.")
                    .define("enableOxygen", true);
            oxygenRate = builder
                    .comment("Ticks per air point lost at max altitude (default: 2). Higher = slower depletion. Scales with atmosphere progress.")
                    .defineInRange("oxygenRate", 2, 1, 100);

            builder.pop();
        }
    }
}
