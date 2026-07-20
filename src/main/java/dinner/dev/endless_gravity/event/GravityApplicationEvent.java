package dinner.dev.endless_gravity.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before Endless Gravity applies an upward offset to an entity in The End.
 * <p>
 * Cancel this event to prevent gravity from being applied entirely.
 * Modify {@link #setOffset(double)} to amplify or reduce the gravity effect.
 * <p>
 * Example: an enchantment mod cancels this event for players wearing levitation boots.
 */
public class GravityApplicationEvent extends Event implements ICancellableEvent {

    private final Entity entity;
    private double offset;

    public GravityApplicationEvent(Entity entity, double offset) {
        this.entity = entity;
        this.offset = offset;
    }

    /**
     * The entity about to have gravity applied.
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * The current gravity offset. Default is the config value for this entity type.
     */
    public double getOffset() {
        return offset;
    }

    /**
     * Set a custom gravity offset. Set to 0 for no gravity effect.
     */
    public void setOffset(double offset) {
        this.offset = offset;
    }
}
