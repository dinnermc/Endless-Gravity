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
        public final ModConfigSpec.DoubleValue noFallDamageAltitude;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGain;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGainHF;
        public final ModConfigSpec.ConfigValue<List<? extends String>> atmosphereLayers;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("gravity");

            endEntityGravity = builder
                    .comment("Master switch for End gravity. Off = vanilla gravity for everything, ignoring the toggles below.")
                    .define("endEntityGravity", true);

            enablePlayerGravity = builder
                    .comment("Reduced gravity for players in The End.")
                    .define("enablePlayerGravity", true);
            playerGravityOffset = builder
                    .comment("Upward impulse per tick for players. 0.055 leaves a gentle float; crank it up and you barely fall at all.")
                    .defineInRange("playerGravityOffset", 0.055, 0.0, 0.07);

            enableItemGravity = builder
                    .comment("Reduced gravity for dropped items in The End.")
                    .define("enableItemGravity", true);
            itemGravityOffset = builder
                    .comment("Same impulse as the player offset, but for items. Lower because items have no way to steer.")
                    .defineInRange("itemGravityOffset", 0.025, 0.0, 0.035);

            enableArrowGravity = builder
                    .comment("Reduced gravity for arrows and tridents in The End.")
                    .define("enableArrowGravity", true);
            arrowGravityOffset = builder
                    .comment("Upward impulse per tick for arrows and tridents.")
                    .defineInRange("arrowGravityOffset", 0.03, 0.0, 0.04);

            enableThrownGravity = builder
                    .comment("Reduced gravity for thrown projectiles (snowballs, pearls, bottles...) in The End.")
                    .define("enableThrownGravity", true);
            thrownGravityOffset = builder
                    .comment("Upward impulse per tick for thrown projectiles.")
                    .defineInRange("thrownGravityOffset", 0.018, 0.0, 0.025);

            builder.pop();

            builder.push("effects");

            enableLowPassFilter = builder
                    .comment("Muffles The End's audio with a low-pass filter, like being underwater.")
                    .define("enableLowPassFilter", true);
            lowPassGain = builder
                    .comment("Filter volume. 0.35 is a subtle dampening; go lower for a heavier blanket.")
                    .defineInRange("lowPassGain", 0.35, 0.0, 1.0);
            lowPassGainHF = builder
                    .comment("How much high-frequency sound survives the filter. Lower = duller, boomier audio.")
                    .defineInRange("lowPassGainHF", 0.25, 0.0, 1.0);

            builder.pop();

            builder.push("gameplay");

            fallDamageMode = builder
                    .comment("Fall damage in The End and the Overworld: 0 = vanilla, 1 = disabled, 2 = velocity-based.")
                    .defineInRange("fallDamageMode", 2, 0, 2);
            fallDamageVelocityScale = builder
                    .comment("Damage multiplier for velocity-based mode. 1.0 = one heart per unit of impact velocity.")
                    .defineInRange("fallDamageVelocityScale", 1.0, 0.1, 10.0);
            fallDamageMinVelocity = builder
                    .comment("Impact speed below which velocity-based mode deals no damage.")
                    .defineInRange("fallDamageMinVelocity", 0.6, 0.0, 5.0);

            enableBlockGravity = builder
                    .comment("Falling blocks (sand, gravel, anvils, dragon eggs) fall slower in The End.")
                    .define("enableBlockGravity", true);
            blockGravityOffset = builder
                    .comment("Upward impulse per tick for falling blocks.")
                    .defineInRange("blockGravityOffset", 0.035, 0.0, 0.1);

            builder.pop();

            builder.push("sable");

            endSableGravity = builder
                    .comment("Master switch for the End Sable physics pack. Off = Sable falls back to its own defaults.")
                    .define("endSableGravity", true);

            sableGravityY = builder
                    .comment("Downward pull in The End. Negative = down; the more negative, the stronger.")
                    .defineInRange("sableGravityY", -4.0, -20.0, 0.0);
            sablePressure = builder
                    .comment("Air pressure in The End. 0 = vacuum.")
                    .defineInRange("sablePressure", 0.0, 0.0, 10.0);
            sableDrag = builder
                    .comment("Air drag in The End. 0 = no resistance.")
                    .defineInRange("sableDrag", 0.05, 0.0, 10.0);
            sableDatapackPriority = builder
                    .comment("Datapack priority for the End pack. Sable's built-ins sit at 1000, so anything above that wins.")
                    .defineInRange("sableDatapackPriority", 9999, 1, 9999);

            builder.pop();

            builder.push("sable_overworld");

            overworldSableGravity = builder
                    .comment("Master switch for the Overworld Sable physics pack. Off = Sable reuses its own defaults.")
                    .define("overworldSableGravity", true);

            overworldSableDatapackPriority = builder
                    .comment("Datapack priority for the Overworld pack. Must beat Sable's built-in 1000.")
                    .defineInRange("overworldSableDatapackPriority", 2000, 1, 9999);

            builder.pop();

            builder.push("atmosphere");

            enableAtmosphere = builder
                    .comment("Turns on the Overworld's altitude physics: gravity eases off above Y=64 and fades to vacuum by Y=3500. Also applies inside Sable sub-levels.")
                    .define("enableAtmosphere", true);
            overworldEntityGravity = builder
                    .comment("Master switch for the atmosphere's effect on entities. Off = full vanilla gravity at every altitude.")
                    .define("overworldEntityGravity", true);
            atmosphereGravityMax = builder
                    .comment("Upward impulse per tick at deep space, ramped from 0 at Y=64. Keep under 0.08 so a residual pull always remains - never perfect zero-G.")
                    .defineInRange("atmosphereGravityMax", 0.075, 0.0, 0.1);
            noFallDamageAltitude = builder
                    .comment("Above this Y, fall damage is cancelled in the Overworld (and Sable sub-levels). 400 = the air is already thin enough that falls stop hurting; 3500 = only at the top of the atmosphere.")
                    .defineInRange("noFallDamageAltitude", 400.0, 64.0, 3500.0);
            atmosphereMuffleGain = builder
                    .comment("Low-pass gain at deep space, interpolated from 1.0 at the base. 0.01 is effectively silence.")
                    .defineInRange("atmosphereMuffleGain", 0.01, 0.0, 1.0);
            atmosphereMuffleGainHF = builder
                    .comment("High-frequency gain at deep space, same interpolation as the gain above.")
                    .defineInRange("atmosphereMuffleGainHF", 0.005, 0.0, 1.0);
            atmosphereLayers = builder
                    .comment("\"altitude:pressure\" pairs defining the pressure curve - 1.0 = full atmosphere, 0.0 = vacuum. Gravity, muffling and the Sable datapack all read this curve. Defaults: -64:1.25, 64:1.0, 400:0.5, 900:0.2, 1200:0.08, 1800:0.01, 2500:0.001, 3500:0.0")
                    .defineListAllowEmpty("atmosphereLayers",
                            List.of("-64:1.25", "64:1.0", "400:0.5", "900:0.2", "1200:0.08", "1800:0.01", "2500:0.001", "3500:0.0"),
                            obj -> obj instanceof String);

            builder.pop();
        }
    }
}