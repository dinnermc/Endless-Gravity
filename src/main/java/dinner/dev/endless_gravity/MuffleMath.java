package dinner.dev.endless_gravity;

/**
 * Pure-Java acoustic math for Endless Gravity's OpenAL low-pass filter.
 *
 * <p>Uses perceptual (logarithmic / decibel) attenuation so high frequencies soften
 * naturally in thin air before overall loudness drops off in near-vacuum, plus a warm
 * suit/bone-conduction curve for the local player's own sounds and temporal smoothing
 * to avoid clicks or sudden jumps.
 */
public final class MuffleMath {

    private static final double MIN_LOG_FLOOR = 1.0e-4;
    private static final float SMOOTH_ALPHA = 0.22f;
    private static final float SNAP_EPSILON = 0.002f;

    private MuffleMath() {}

    /**
     * Perceptual overall-volume gain at atmospheric thinning progress {@code [0, 1]}
     * (0 = full atmosphere, 1 = vacuum). Holds up through thin air and drops cleanly
     * into vacuum.
     */
    public static double perceptualGain(double progress, double minGain) {
        double p = clamp01(progress);
        double floor = clamp01(minGain);
        if (p <= 0.0 || floor >= 1.0) return 1.0;
        if (p >= 1.0) return floor;
        double shaped = Math.pow(p, 1.25);
        double safeFloor = Math.max(floor, MIN_LOG_FLOOR);
        double value = Math.pow(safeFloor, shaped);
        return Math.max(floor, Math.min(1.0, value));
    }

    /**
     * Perceptual high-frequency gain at atmospheric thinning progress {@code [0, 1]}.
     * Highs begin rolling off earlier than overall loudness for realistic thin-air acoustics.
     */
    public static double perceptualGainHF(double progress, double minGainHF) {
        double p = clamp01(progress);
        double floor = clamp01(minGainHF);
        if (p <= 0.0 || floor >= 1.0) return 1.0;
        if (p >= 1.0) return floor;
        double shaped = Math.pow(p, 0.65);
        double safeFloor = Math.max(floor, MIN_LOG_FLOOR);
        double value = Math.pow(safeFloor, shaped);
        return Math.max(floor, Math.min(1.0, value));
    }

    /**
     * Suit/helmet bone-conduction gain for the local player's own sounds (footsteps, eating,
     * armor, hurt). Stays audible in vacuum while external world sounds fade near silence.
     */
    public static float selfSuitGain(float worldGain) {
        float w = clamp01f(worldGain);
        if (w >= 0.999f) return 1.0f;
        return Math.max(w, 0.35f + 0.65f * w);
    }

    /**
     * Suit/helmet high-frequency rolloff for the local player's own sounds: warm, bassy
     * helmet conduction without disappearing completely.
     */
    public static float selfSuitGainHF(float worldGainHF) {
        float w = clamp01f(worldGainHF);
        if (w >= 0.999f) return 1.0f;
        return Math.max(w, Math.min(1.0f, 0.08f + 0.30f * w));
    }

    /** Exponential temporal smoothing per 20Hz tick with clean snapping at endpoints. */
    public static float smooth(float current, float target) {
        float t = clamp01f(target);
        float c = clamp01f(current);
        float diff = t - c;
        if (Math.abs(diff) <= SNAP_EPSILON) {
            return t;
        }
        return clamp01f(c + diff * SMOOTH_ALPHA);
    }

    public static double clamp01(double v) {
        if (!Double.isFinite(v)) return 1.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    public static float clamp01f(float v) {
        if (!Float.isFinite(v)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
