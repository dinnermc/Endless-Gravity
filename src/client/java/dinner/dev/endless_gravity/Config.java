package dinner.dev.endless_gravity;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * NeoForge-specific config using ModConfigSpec.
 */
public class Config {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {
        final ModConfigSpec.BooleanValue endEntityGravity;
        final ModConfigSpec.BooleanValue enablePlayerGravity;
        final ModConfigSpec.DoubleValue playerGravityOffset;
        final ModConfigSpec.BooleanValue enableItemGravity;
        final ModConfigSpec.DoubleValue itemGravityOffset;
        final ModConfigSpec.BooleanValue enableArrowGravity;
        final ModConfigSpec.DoubleValue arrowGravityOffset;
        final ModConfigSpec.BooleanValue enableThrownGravity;
        final ModConfigSpec.DoubleValue thrownGravityOffset;

        // Effects
        final ModConfigSpec.BooleanValue enableLowPassFilter;
        final ModConfigSpec.DoubleValue lowPassGain;
        final ModConfigSpec.DoubleValue lowPassGainHF;

        // Particles
        final ModConfigSpec.DoubleValue particleGravityOffset;

        // Gameplay
        final ModConfigSpec.IntValue fallDamageMode;
        final ModConfigSpec.BooleanValue enableBlockGravity;
        final ModConfigSpec.DoubleValue blockGravityOffset;

        // Atmosphere
        final ModConfigSpec.BooleanValue enableAtmosphere;
        final ModConfigSpec.BooleanValue overworldEntityGravity;
        final ModConfigSpec.DoubleValue atmosphereGravityMax;
        final ModConfigSpec.DoubleValue noFallDamageAltitude;
        final ModConfigSpec.DoubleValue atmosphereMuffleGain;
        final ModConfigSpec.DoubleValue atmosphereMuffleGainHF;
        final ModConfigSpec.ConfigValue<List<? extends String>> atmosphereLayers;

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
                    .comment("Fall damage in The End and Sable dimensions: 0 = vanilla, 1 = disabled.")
                    .defineInRange("fallDamageMode", 1, 0, 1);

            enableBlockGravity = builder
                    .comment("Falling blocks (sand, gravel, anvils, dragon eggs) fall slower in The End.")
                    .define("enableBlockGravity", true);
            blockGravityOffset = builder
                    .comment("Upward impulse per tick for falling blocks.")
                    .defineInRange("blockGravityOffset", 0.035, 0.0, 0.1);

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
            particleGravityOffset = builder
                    .comment("Upward acceleration per tick for particles in The End. 0.08 = neutral gravity (float), <0.08 = slow fall, >0.08 = slow rise")
                    .defineInRange("particleGravityOffset", 0.5, 0.0, 0.5);
            atmosphereLayers = builder
                    .comment("\"altitude:pressure\" pairs defining the pressure curve - 1.0 = full atmosphere, 0.0 = vacuum. Gravity, muffling and the Sable datapack all read this curve. Defaults: -64:1.25, 64:1.0, 400:0.5, 900:0.2, 1200:0.08, 1800:0.01, 2500:0.001, 3500:0.0")
                    .defineListAllowEmpty("atmosphereLayers",
                            java.util.Arrays.asList("-64:1.25", "64:1.0", "400:0.5", "900:0.2", "1200:0.08", "1800:0.01", "2500:0.001", "3500:0.0"),
                            obj -> obj instanceof String);

            builder.pop();
        }
    }
}