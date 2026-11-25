package dinner.dev.endless_gravity;

import java.util.List;

/** Gameplay settings for one dimension. */
public final class CustomDimConfig {
    public static final int MAX_ATMOSPHERE_LAYERS = 64;

    public enum Mode {
        ATMOSPHERE,
        FIXED_GRAVITY
    }

    public enum FallDamageMode {
        INHERIT,
        NORMAL,
        PROPORTIONAL,
        DISABLED
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
    private final double surfaceGravityOffset;
    private final boolean atmosphereAffectNonLiving;
    private final FallDamageMode fallDamageMode;
    private final double fixedMuffleGain;
    private final double fixedMuffleGainHF;
    private final double atmosphereMuffleStartAltitude;

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
        this(dimensionId, mode,
                playerGravityOffset, mobGravityOffset, itemGravityOffset,
                arrowGravityOffset, thrownGravityOffset, blockGravityOffset,
                atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude,
                atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers,
                0.0, false, FallDamageMode.INHERIT);
    }

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
            List<String> atmosphereLayers,
            double surfaceGravityOffset,
            boolean atmosphereAffectNonLiving,
            FallDamageMode fallDamageMode) {
        this(dimensionId, mode,
                playerGravityOffset, mobGravityOffset, itemGravityOffset,
                arrowGravityOffset, thrownGravityOffset, blockGravityOffset,
                atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude,
                atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers,
                surfaceGravityOffset, atmosphereAffectNonLiving, fallDamageMode,
                1.0, 1.0);
    }

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
            List<String> atmosphereLayers,
            double surfaceGravityOffset,
            boolean atmosphereAffectNonLiving,
            FallDamageMode fallDamageMode,
            double fixedMuffleGain,
            double fixedMuffleGainHF) {
        this(dimensionId, mode,
                playerGravityOffset, mobGravityOffset, itemGravityOffset,
                arrowGravityOffset, thrownGravityOffset, blockGravityOffset,
                atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude,
                atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers,
                surfaceGravityOffset, atmosphereAffectNonLiving, fallDamageMode,
                fixedMuffleGain, fixedMuffleGainHF, AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE);
    }

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
            List<String> atmosphereLayers,
            double surfaceGravityOffset,
            boolean atmosphereAffectNonLiving,
            FallDamageMode fallDamageMode,
            double fixedMuffleGain,
            double fixedMuffleGainHF,
            double atmosphereMuffleStartAltitude) {
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
        this.parsedAtmosphereLayers = List.copyOf(AtmosphereLayers.parseUncached(this.atmosphereLayers));
        this.surfaceGravityOffset = surfaceGravityOffset;
        this.atmosphereAffectNonLiving = atmosphereAffectNonLiving;
        this.fallDamageMode = fallDamageMode == null ? FallDamageMode.INHERIT : fallDamageMode;
        this.fixedMuffleGain = MuffleMath.clamp01(fixedMuffleGain);
        this.fixedMuffleGainHF = MuffleMath.clamp01(fixedMuffleGainHF);
        this.atmosphereMuffleStartAltitude = Double.isFinite(atmosphereMuffleStartAltitude)
                ? AtmosphereLayers.clamp(atmosphereMuffleStartAltitude, -2048.0, 30000.0)
                : AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE;
    }

    public String getDimensionId() { return dimensionId; }
    public Mode getMode() { return mode; }
    public boolean isEnableCustomGravity() { return mode == Mode.FIXED_GRAVITY; }
    public boolean isEnableAtmosphere() { return mode == Mode.ATMOSPHERE; }
    public boolean isEnableMobGravity() { return mode == Mode.FIXED_GRAVITY; }
    public double getPlayerGravityOffset() { return playerGravityOffset; }
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
    public double getSurfaceGravityOffset() { return surfaceGravityOffset; }
    public boolean isAtmosphereAffectNonLiving() { return atmosphereAffectNonLiving; }
    public FallDamageMode getFallDamageMode() { return fallDamageMode; }
    public double getFixedMuffleGain() { return fixedMuffleGain; }
    public double getFixedMuffleGainHF() { return fixedMuffleGainHF; }
    public boolean hasFixedAudioMuffle() { return fixedMuffleGain < 0.999 || fixedMuffleGainHF < 0.999; }
    public double getAtmosphereMuffleStartAltitude() { return atmosphereMuffleStartAltitude; }

    public List<AtmosphereLayers.Layer> getParsedAtmosphereLayers() {
        return parsedAtmosphereLayers;
    }

    public double getAtmospherePressure(double y) {
        return AtmosphereLayers.getPressure(getParsedAtmosphereLayers(), y);
    }

    public double getAtmosphereProgress(double y) {
        return AtmosphereLayers.getProgress(getParsedAtmosphereLayers(), y);
    }

    public double getAtmosphereGravityProgress(double y) {
        return AtmosphereLayers.getGravityProgress(
                getParsedAtmosphereLayers(), atmosphereBaseAltitude, y);
    }

    /**
     * Upward gravity offset at altitude {@code y} in atmosphere mode: starts at
     * {@link #getSurfaceGravityOffset()} and ramps smoothly to {@code max(surface, atmosphereGravityMax)}.
     */
    public double getAtmosphereGravityOffset(double y) {
        double progress = getAtmosphereGravityProgress(y);
        if (surfaceGravityOffset == 0.0) {
            return progress * atmosphereGravityMax;
        }
        double target = Math.max(surfaceGravityOffset, atmosphereGravityMax);
        return surfaceGravityOffset + progress * (target - surfaceGravityOffset);
    }

    public double getAtmosphereMuffleProgress(double y) {
        return AtmosphereLayers.getAudioMuffleProgress(
                getParsedAtmosphereLayers(), atmosphereMuffleStartAltitude, y);
    }

    public double getAtmosphereMuffleGain(double y) {
        return MuffleMath.perceptualGain(getAtmosphereMuffleProgress(y), atmosphereMuffleGain);
    }

    public double getAtmosphereMuffleGainHF(double y) {
        return MuffleMath.perceptualGainHF(getAtmosphereMuffleProgress(y), atmosphereMuffleGainHF);
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
