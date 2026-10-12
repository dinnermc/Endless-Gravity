package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class CustomDimConfig {
    private final String dimensionId;
    private final boolean enableCustomGravity;
    private final double mobGravityOffset;
    private final boolean enableAtmosphere;
    private final double atmosphereGravityMax;
    private final double atmosphereBaseAltitude;
    private final double atmosphereMaxAltitude;
    private final double atmosphereMuffleGain;
    private final double atmosphereMuffleGainHF;
    private final boolean enableMobGravity;
    private final double mobGravityOffsetCustom;
    private final List<String> atmosphereLayers;

    public CustomDimConfig(
            String dimensionId,
            boolean enableCustomGravity,
            double mobGravityOffset,
            boolean enableAtmosphere,
            double atmosphereGravityMax,
            double atmosphereBaseAltitude,
            double atmosphereMaxAltitude,
            double atmosphereMuffleGain,
            double atmosphereMuffleGainHF,
            boolean enableMobGravity,
            double mobGravityOffsetCustom,
            List<String> atmosphereLayers) {
        this.dimensionId = dimensionId;
        this.enableCustomGravity = enableCustomGravity;
        this.mobGravityOffset = mobGravityOffset;
        this.enableAtmosphere = enableAtmosphere;
        this.atmosphereGravityMax = atmosphereGravityMax;
        this.atmosphereBaseAltitude = atmosphereBaseAltitude;
        this.atmosphereMaxAltitude = atmosphereMaxAltitude;
        this.atmosphereMuffleGain = atmosphereMuffleGain;
        this.atmosphereMuffleGainHF = atmosphereMuffleGainHF;
        this.enableMobGravity = enableMobGravity;
        this.mobGravityOffsetCustom = mobGravityOffsetCustom;
        this.atmosphereLayers = atmosphereLayers;
    }

    public String getDimensionId() { return dimensionId; }
    public boolean isEnableCustomGravity() { return enableCustomGravity; }
    public double getMobGravityOffset() { return mobGravityOffset; }
    public boolean isEnableAtmosphere() { return enableAtmosphere; }
    public double getAtmosphereGravityMax() { return atmosphereGravityMax; }
    public double getAtmosphereBaseAltitude() { return atmosphereBaseAltitude; }
    public double getAtmosphereMaxAltitude() { return atmosphereMaxAltitude; }
    public double getAtmosphereMuffleGain() { return atmosphereMuffleGain; }
    public double getAtmosphereMuffleGainHF() { return atmosphereMuffleGainHF; }
    public boolean isEnableMobGravity() { return enableMobGravity; }
    public double getMobGravityOffsetCustom() { return mobGravityOffsetCustom; }
    public List<String> getAtmosphereLayers() { return atmosphereLayers; }

    public static CustomDimConfig vanilla() {
        return new CustomDimConfig(
                "minecraft:overworld",
                false, 0.0, false, 0.075, 64.0, 3500.0, 0.01, 0.005,
                false, 0.025, List.of()
        );
    }

    public static CustomDimConfig endDefault() {
        return new CustomDimConfig(
                "minecraft:the_end",
                true, 0.025, false, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, 0.025, List.of()
        );
    }
}