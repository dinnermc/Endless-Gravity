package dinner.dev.endless_gravity.api;

/**
 * The "calculate gravity" event. Registered modifiers run every time Endless Gravity
 * resolves an offset for an entity, and may read the context and change or cancel it.
 *
 * <p>Modifiers run:
 * <ol>
 *     <li>Global modifiers in descending priority order (higher priority first).</li>
 *     <li>Then, if present, the entity's own modifier (see
 *     {@link GravityApi#setEntityModifier}).</li>
 * </ol>
 *
 * <p>Typical uses:
 * <pre>{@code
 * GravityApi.registerModifier(id("mymod:low_gravity_zone"), 100, ctx -> {
 *     if (ctx.entity().getY() > 1000) {
 *         ctx.addOffset(0.02);      // push them up a little
 *     }
 *     if (ctx.entity().isUnderWater()) {
 *         ctx.cancel();             // leave vanilla gravity alone
 *     }
 * });
 * }</pre>
 *
 * <p>The modifier must be cheap: it is invoked for every candidate entity, every tick.
 */
@FunctionalInterface
public interface GravityModifier {

    /**
     * @param context mutable calculation context; change it with
     *                {@link GravityContext#setOffset}, {@link GravityContext#addOffset}
     *                or {@link GravityContext#cancel()}
     */
    void modify(GravityContext context);
}
