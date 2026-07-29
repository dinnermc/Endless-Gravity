package dinner.dev.endless_gravity.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class GravityImmunityEvent extends Event implements ICancellableEvent {

    private final Entity entity;
    private boolean immune;

    public GravityImmunityEvent(Entity entity, boolean immune) {
        this.entity = entity;
        this.immune = immune;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isImmune() {
        return immune;
    }

    public void setImmune(boolean immune) {
        this.immune = immune;
    }
}
