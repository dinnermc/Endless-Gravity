package dinner.dev.endless_gravity.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Mutable audio-muffling calculation context passed to every registered
 * {@link AudioMuffleModifier}.
 *
 * <p>Gain values are in {@code [0.0, 1.0]} where {@code 1.0} means completely clear
 * (unfiltered) audio and values closer to {@code 0.0} mean stronger attenuation.
 * Calling {@link #cancel()} restores clear audio ({@code 1.0 / 1.0}) — useful for
 * pressurized spacecraft cabins, airlocks, or sealed habitats.
 */
public final class AudioMuffleContext {

    private final Level level;
    private final Entity listener;
    private final double altitude;
    private final double baseGain;
    private final double baseGainHF;

    private double gain;
    private double gainHF;
    private boolean cancelled;

    public AudioMuffleContext(Level level, Entity listener, double altitude, double baseGain, double baseGainHF) {
        this.level = level;
        this.listener = listener;
        this.altitude = altitude;
        this.baseGain = clamp01(baseGain);
        this.baseGainHF = clamp01(baseGainHF);
        this.gain = this.baseGain;
        this.gainHF = this.baseGainHF;
    }

    public Level level() { return level; }
    public Entity listener() { return listener; }
    public double altitude() { return altitude; }
    public double baseGain() { return baseGain; }
    public double baseGainHF() { return baseGainHF; }

    public double gain() { return gain; }
    public double gainHF() { return gainHF; }
    public boolean isCancelled() { return cancelled; }

    public void setGain(double gain) {
        this.gain = clamp01(gain);
    }

    public void setGainHF(double gainHF) {
        this.gainHF = clamp01(gainHF);
    }

    /**
     * Scales the current muffling intensity toward clear audio ({@code 1.0}).
     * {@code strength = 0.0} removes all muffling; {@code strength = 0.5} halves it;
     * {@code strength = 1.0} leaves it unchanged.
     */
    public void scaleMuffle(double strength) {
        double s = clamp01(strength);
        this.gain = 1.0 - (1.0 - this.gain) * s;
        this.gainHF = 1.0 - (1.0 - this.gainHF) * s;
    }

    /** Suppresses all audio muffling for this tick (transitions smoothly back to clear audio). */
    public void cancel() {
        this.cancelled = true;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public float effectiveGain() {
        return cancelled ? 1.0f : (float) gain;
    }

    public float effectiveGainHF() {
        return cancelled ? 1.0f : (float) gainHF;
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 1.0;
        return Math.max(0.0, Math.min(1.0, v));
    }
}
