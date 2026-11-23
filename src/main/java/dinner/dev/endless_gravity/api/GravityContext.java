package dinner.dev.endless_gravity.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Mutable description of one gravity calculation for one entity on one tick.
 *
 * <p>The context starts with the base offset Endless Gravity would apply (see
 * {@link #source()}), and {@link GravityModifier}s may then change it.
 */
public final class GravityContext {

    private final Entity entity;
    private final Level level;
    private final GravityChannel channel;

    private GravitySource source = GravitySource.NONE;
    private double offset;
    private boolean cancelled;

    public GravityContext(Entity entity, Level level, GravityChannel channel) {
        this.entity = entity;
        this.level = level;
        this.channel = channel;
    }

    /** The entity the offset is being calculated for. */
    public Entity entity() {
        return entity;
    }

    /** The level the entity is in. */
    public Level level() {
        return level;
    }

    /** The channel this calculation belongs to. */
    public GravityChannel channel() {
        return channel;
    }

    /** Where the base offset came from (before modifiers ran). */
    public GravitySource source() {
        return source;
    }

    /** The current offset (base, plus anything modifiers added). Upward positive. */
    public double offset() {
        return offset;
    }

    /** True if a modifier cancelled the calculation. */
    public boolean isCancelled() {
        return cancelled;
    }

    /** True if Endless Gravity should apply {@link #offset()} this tick. */
    public boolean applicable() {
        return !cancelled && offset != 0.0;
    }

    /** Set the base source. Intended for Endless Gravity itself. */
    public void setSource(GravitySource source) {
        this.source = source;
    }

    /** Replace the offset. */
    public void setOffset(double offset) {
        this.offset = offset;
    }

    /** Add to (or subtract from) the offset. */
    public void addOffset(double delta) {
        this.offset += delta;
    }

    /** The entity's vanilla/attribute base gravity before Endless Gravity's offset. */
    public double baseGravity() {
        return entity.getGravity();
    }

    /**
     * Effective downward gravity after applying {@link #offset()}, clamped to {@code >= 0.0}.
     * Returns {@link #baseGravity()} if this calculation is cancelled.
     */
    public double effectiveGravity() {
        if (cancelled) return baseGravity();
        return Math.max(0.0, baseGravity() - offset);
    }

    /**
     * Set the offset so the entity experiences {@code factor * baseGravity()} downward pull
     * (for example, {@code 0.165} for lunar gravity, {@code 0.0} for zero-G, {@code 2.0} for
     * double gravity). Works consistently across all {@link GravityChannel}s.
     */
    public void scaleGravity(double factor) {
        if (!Double.isFinite(factor)) return;
        this.offset = baseGravity() * (1.0 - factor);
    }

    /** Cancel the calculation: Endless Gravity leaves vanilla gravity untouched. */
    public void cancel() {
        this.cancelled = true;
    }

    /** Undo a previous {@link #cancel()} call (for higher-authority overrides). */
    public void uncancel() {
        this.cancelled = false;
    }
}
