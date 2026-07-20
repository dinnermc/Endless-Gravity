package dinner.dev.endless_gravity.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired when velocity-based fall damage is calculated in The End (fall damage mode 2).
 * <p>
 * Cancel this event to prevent fall damage entirely for this tick.
 * Modify {@link #setDamageMultiplier(float)} and {@link #setDistance(float)} to change the damage.
 * <p>
 * Example: an armor mod reduces damage for players wearing feather falling enchantment.
 */
public class FallDamageCalculationEvent extends Event implements ICancellableEvent {

    private final ServerPlayer player;
    private float damageMultiplier;
    private float distance;

    public FallDamageCalculationEvent(ServerPlayer player, float damageMultiplier, float distance) {
        this.player = player;
        this.damageMultiplier = damageMultiplier;
        this.distance = distance;
    }

    /**
     * The player taking fall damage.
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * The damage multiplier. Default is 1.0 for velocity-based mode.
     */
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Set the damage multiplier. Lower values reduce damage.
     */
    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    /**
     * The calculated fall distance used for damage. Default is velocity * scale + 3.0.
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Set the fall distance. Higher values increase damage.
     */
    public void setDistance(float distance) {
        this.distance = distance;
    }
}
