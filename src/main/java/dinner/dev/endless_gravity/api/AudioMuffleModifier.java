package dinner.dev.endless_gravity.api;

/**
 * Callback that can inspect, scale, override, or cancel Endless Gravity's client-side
 * OpenAL low-pass audio filter (for example, inside pressurized cabins or helmets).
 *
 * <p>Register via {@link GravityApi#registerAudioMuffleModifier}.
 */
@FunctionalInterface
public interface AudioMuffleModifier {
    void modify(AudioMuffleContext context);
}
