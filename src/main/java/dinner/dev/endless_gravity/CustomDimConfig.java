package dinner.dev.endless_gravity;

import java.util.List;

/** Gameplay settings for one dimension. */
public final class CustomDimConfig {
    public static final int MAX_ATMOSPHERE_LAYERS = 64;

    public enum Mode {
        ATMOSPHERE,
        FIXED_GRAVITY
    }

    private final String dimensionId;
    private final Mode mode;
    private final double playerGravityOffset;
    private final double mobGravityOffset;
    private final double itemGravityOffset;
    private final double arrowGravityOffset;
    private final double thrownGravityOffset;
    private final double blockGravityOffset;
    private final double atmosphereGravityMax;
    private final double atmosphereBaseAltitude;
    private final Double noFallDamageAltitude;
    private final double atmosphereMuffleGain;
    private final double atmosphereMuffleGainHF;
    private final List<String> atmosphereLayers;
    private final List<AtmosphereLayers.Layer> parsedAtmosphereLayers;

    public CustomDimConfig(
            String dimensionId,
            Mode mode,
            double playerGravityOffset,
            double mobGravityOffset,
            double itemGravityOffset,
            double arrowGravityOffset,
            double thrownGravityOffset,
            double blockGravityOffset,
            double atmosphereGravityMax,
            double atmosphereBaseAltitude,
            Double noFallDamageAltitude,
            double atmosphereMuffleGain,
            double atmosphereMuffleGainHF,
            List<String> atmosphereLayers) {
        this.dimensionId = dimensionId;
        this.mode = mode;
        this.playerGravityOffset = playerGravityOffset;
        this.mobGravityOffset = mobGravityOffset;
        this.itemGravityOffset = itemGravityOffset;
        this.arrowGravityOffset = arrowGravityOffset;
        this.thrownGravityOffset = thrownGravityOffset;
        this.blockGravityOffset = blockGravityOffset;
        this.atmosphereGravityMax = atmosphereGravityMax;
        this.atmosphereBaseAltitude = atmosphereBaseAltitude;
        this.noFallDamageAltitude = noFallDamageAltitude;
        this.atmosphereMuffleGain = atmosphereMuffleGain;
        this.atmosphereMuffleGainHF = atmosphereMuffleGainHF;
        this.atmosphereLayers = List.copyOf(atmosphereLayers);
        this.parsedAtmosphereLayers = AtmosphereLayers.parseUncached(this.atmosphereLayers);
    }

    public String getDimensionId() { return dimensionId; }
    public Mode getMode() { return mode; }
    public boolean isEnableCustomGravity() { return mode == Mode.FIXED_GRAVITY; }
    public boolean isEnableAtmosphere() { return mode == Mode.ATMOSPHERE; }
    public boolean isEnableMobGravity() { return mode == Mode.FIXED_GRAVITY; }
    public Double getPlayerGravityOffset() { return playerGravityOffset; }
    public double getMobGravityOffset() { return mobGravityOffset; }
    public double getItemGravityOffset() { return itemGravityOffset; }
    public double getArrowGravityOffset() { return arrowGravityOffset; }
    public double getThrownGravityOffset() { return thrownGravityOffset; }
    public double getBlockGravityOffset() { return blockGravityOffset; }
    public double getAtmosphereGravityMax() { return atmosphereGravityMax; }
    public double getAtmosphereBaseAltitude() { return atmosphereBaseAltitude; }
    public Double getNoFallDamageAltitude() { return noFallDamageAltitude; }
    public double getAtmosphereMuffleGain() { return atmosphereMuffleGain; }
    public double getAtmosphereMuffleGainHF() { return atmosphereMuffleGainHF; }
    public List<String> getAtmosphereLayers() { return atmosphereLayers; }

    public List<AtmosphereLayers.Layer> getParsedAtmosphereLayers() {
        return parsedAtmosphereLayers;
    }

    public double getAtmosphereProgress(double y) {
        return AtmosphereLayers.getProgress(getParsedAtmosphereLayers(), y);
    }

    public double getAtmosphereGravityProgress(double y) {
        return AtmosphereLayers.getGravityProgress(
                getParsedAtmosphereLayers(), atmosphereBaseAltitude, y);
    }

    public double getAtmosphereGravityOffset(double y) {
        return getAtmosphereGravityProgress(y) * atmosphereGravityMax;
    }

    public double getAtmosphereMuffleGain(double y) {
        return 1.0 - getAtmosphereProgress(y) * (1.0 - atmosphereMuffleGain);
    }

    public double getAtmosphereMuffleGainHF(double y) {
        return 1.0 - getAtmosphereProgress(y) * (1.0 - atmosphereMuffleGainHF);
    }

    public double getAtmosphereMaxAltitude() {
        return parsedAtmosphereLayers.get(parsedAtmosphereLayers.size() - 1).altitude();
    }

    /** Compatibility accessors retained for existing integrations. */
    public double getMobGravityOffsetCustom() { return mobGravityOffset; }
    public double getMobGravityOffsetForDimension() { return mobGravityOffset; }

    /** Legacy constructor retained for integrations compiled against the old model. */
    @Deprecated
    public CustomDimConfig(
            String dimensionId,
            boolean enableCustomGravity,
            double mobGravityOffset,
            Double playerGravityOffset,
            boolean enableAtmosphere,
            double atmosphereGravityMax,
            double atmosphereBaseAltitude,
            double atmosphereMaxAltitude,
            double atmosphereMuffleGain,
            double atmosphereMuffleGainHF,
            boolean enableMobGravity,
            double mobGravityOffsetCustom,
            List<String> atmosphereLayers) {
        this(dimensionId,
                enableAtmosphere ? Mode.ATMOSPHERE : Mode.FIXED_GRAVITY,
                playerGravityOffset == null ? 0.0 : playerGravityOffset,
                enableMobGravity ? mobGravityOffsetCustom : mobGravityOffset,
                0.0, 0.0, 0.0, 0.0,
                atmosphereGravityMax, atmosphereBaseAltitude,
                enableAtmosphere ? atmosphereMaxAltitude : null,
                atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers);
    }

}
