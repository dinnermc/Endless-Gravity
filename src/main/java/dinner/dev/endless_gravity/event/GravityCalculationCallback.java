package dinner.dev.endless_gravity.event;

import dinner.dev.endless_gravity.api.GravityContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired after the base offset is resolved and before {@link dinner.dev.endless_gravity.api.GravityModifier}s run.
 *
 * <p>Register with:
 * <pre>{@code
 * GravityCalculationCallback.EVENT.register(context -> {
 *     if (someCondition) context.cancel();
 * });
 * }</pre>
 *
 * <p>For a loader-agnostic alternative, see
 * {@link dinner.dev.endless_gravity.api.GravityApi#registerModifier}.
 */
@FunctionalInterface
public interface GravityCalculationCallback {

    net.fabricmc.fabric.api.event.Event<GravityCalculationCallback> EVENT =
            net.fabricmc.fabric.api.event.EventFactory.createArrayBacked(
                    GravityCalculationCallback.class,
                    listeners -> context -> {
                        for (GravityCalculationCallback listener : listeners) {
                            listener.calculate(context);
                        }
                    });

    /** Called for every resolved gravity calculation. Mutate {@code context} to change it. */
    void calculate(GravityContext context);
}
