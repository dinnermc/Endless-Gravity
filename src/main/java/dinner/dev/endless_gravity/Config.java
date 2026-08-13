package dinner.dev.endless_gravity;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

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
        public final ModConfigSpec.BooleanValue endEntityGravity;

        // Gravity values
        public final ModConfigSpec.DoubleValue playerGravityOffset;
        public final ModConfigSpec.DoubleValue itemGravityOffset;
        public final ModConfigSpec.DoubleValue arrowGravityOffset;
        public final ModConfigSpec.DoubleValue thrownGravityOffset;

        // Effects toggles
        public final ModConfigSpec.BooleanValue enableLowPassFilter;

        // Effects values
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
        public final ModConfigSpec.BooleanValue endSableGravity;

        // Sable Overworld
        public final ModConfigSpec.IntValue overworldSableDatapackPriority;
        public final ModConfigSpec.BooleanValue overworldSableGravity;

        // Atmosphere
        public final ModConfigSpec.BooleanValue enableAtmosphere;
        public final ModConfigSpec.BooleanValue overworldEntityGravity;
        public final ModConfigSpec.DoubleValue atmosphereGravityMax;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGain;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGainHF;
        public final ModConfigSpec.ConfigValue<List<? extends String>> atmosphereLayers;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("gravity");

            endEntityGravity = builder
                    .comment("Master toggle: completely disable all entity gravity reduction in The End (default: true). Overrides the individual toggles below.")
                    .define("endEntityGravity", true);

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

            enableLowPassFilter = builder
                    .comment("Enable low-pass audio filter in The End (default: true). Creates a muffled underwater-like sound.")
                    .define("enableLowPassFilter", true);
            lowPassGain = builder
                    .comment("Low-pass filter volume (default: 0.35). Lower = more muffled.")
                    .defineInRange("lowPassGain", 0.35, 0.0, 1.0);
            lowPassGainHF = builder
                    .comment("Low-pass filter high-frequency volume (default: 0.25). Lower = less high-frequency sound.")
                    .defineInRange("lowPassGainHF", 0.25, 0.0, 1.0);

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
                    .defineInRange("blockGravityOffset", 0.035, 0.0, 0.1);

            builder.pop();

            builder.push("sable");

            endSableGravity = builder
                    .comment("Master toggle: completely disable Sable gravity changes in The End (default: true). When disabled, The End uses Sable's default gravity.")
                    .define("endSableGravity", true);

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
                    .comment("Sable datapack priority for The End (default: 9999). Must be > 1000 to override Sable built-in defaults.")
                    .defineInRange("sableDatapackPriority", 9999, 1, 9999);

            builder.pop();

            builder.push("sable_overworld");

            overworldSableGravity = builder
                    .comment("Master toggle: completely disable Sable gravity changes in the Overworld (default: true). When disabled, the Overworld uses Sable's default gravity.")
                    .define("overworldSableGravity", true);

            overworldSableDatapackPriority = builder
                    .comment("Sable datapack priority for the Overworld (default: 2000). Must be > 1000 to override Sable built-in defaults.")
                    .defineInRange("overworldSableDatapackPriority", 2000, 1, 9999);

            builder.pop();

            builder.push("atmosphere");

            enableAtmosphere = builder
                    .comment("Enable atmospheric gravity in the Overworld (default: true). Gravity, muffled audio and drag scale with real atmospheric layers from Y=64 (BASE) to Y=3500 (deep space). Also affects Sable sub-levels.")
                    .define("enableAtmosphere", true);
            overworldEntityGravity = builder
                    .comment("Master toggle: completely disable entity gravity reduction in the Overworld atmosphere (default: true). When disabled, entities keep full gravity at any altitude.")
                    .define("overworldEntityGravity", true);
            atmosphereGravityMax = builder
                    .comment("Maximum upward force per tick at deep space (default: 0.075). Interpolated from 0.0 at BASE Y=64. Higher = less gravity. Keep slightly below vanilla 0.08 so a tiny residual pull remains instead of perfect zero-G.")
                    .defineInRange("atmosphereGravityMax", 0.075, 0.0, 0.1);
            atmosphereMuffleGain = builder
                    .comment("Low-pass filter gain at deep space Y=3500 (default: 0.01). Lower = more muffled. Interpolated from 1.0 at BASE.")
                    .defineInRange("atmosphereMuffleGain", 0.01, 0.0, 1.0);
            atmosphereMuffleGainHF = builder
                    .comment("Low-pass filter high-frequency gain at deep space (default: 0.005). Lower = less high-frequency sound.")
                    .defineInRange("atmosphereMuffleGainHF", 0.005, 0.0, 1.0);
            atmosphereLayers = builder
                    .comment("Atmospheric layers as \"altitude:pressure\" pairs - the universal atmosphere controller. The pressure curve drives custom gravity and levitation (progress = 1 - pressure), muffled audio and the Sable Overworld datapack pressure_function. Pressure 1.0 at base = full atmosphere, 0.0 at deep space = vacuum. Default: -64:1.25, 64:1.0, 400:0.5, 900:0.2, 1200:0.08, 1800:0.01, 2500:0.001, 3500:0.0")
                    .defineList("atmosphereLayers",
                            List.of("-64:1.25", "64:1.0", "400:0.5", "900:0.2", "1200:0.08", "1800:0.01", "2500:0.001", "3500:0.0"),
                            obj -> obj instanceof String);

            builder.pop();
        }
    }
}
