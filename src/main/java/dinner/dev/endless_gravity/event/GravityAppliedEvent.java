package dinner.dev.endless_gravity.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

public class GravityAppliedEvent extends Event {

    private final Entity entity;
    private final double offset;

    public GravityAppliedEvent(Entity entity, double offset) {
        this.entity = entity;
        this.offset = offset;
    }

    public Entity getEntity() {
        return entity;
    }

    public double getOffset() {
        return offset;
    }
}
