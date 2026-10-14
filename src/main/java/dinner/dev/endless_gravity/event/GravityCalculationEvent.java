package dinner.dev.endless_gravity.event;

import dinner.dev.endless_gravity.api.GravityContext;
import net.neoforged.bus.api.Event;

/**
 * Fired on the NeoForge event bus every time Endless Gravity calculates a gravity
 * offset, after the base offset is resolved and <b>before</b> {@code GravityModifier}s
 * run. Listeners receive the mutable {@link GravityContext} and may change or cancel it.
 *
 * <pre>{@code
 * NeoForge.EVENT_BUS.addListener(GravityCalculationEvent.class, event -> {
 *     GravityContext ctx = event.getContext();
 *     if (someCondition) context.cancel();
 * });
 * }</pre>
 *
 * <p>For a loader-agnostic alternative, see
 * {@link dinner.dev.endless_gravity.api.GravityApi#registerModifier}.
 */
public class GravityCalculationEvent extends Event {

    private final GravityContext context;

    public GravityCalculationEvent(GravityContext context) {
        this.context = context;
    }

    /** The calculation being resolved. Modifying it changes what the mod applies this tick. */
    public GravityContext getContext() {
        return context;
    }
}
