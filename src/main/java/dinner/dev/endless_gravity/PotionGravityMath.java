package dinner.dev.endless_gravity;

/**
 * Ambient-gravity-aware scaling for the Weightlessness and Heaviness potion effects.
 *
 * <ul>
 *     <li><b>Weightlessness</b> scales proportionally with the remaining effective gravity
 *         ({@code currentEffective / base}), so it has less absolute effect in low gravity
 *         (and never pushes an entity into upward/negative gravity) and stronger relief in
 *         high gravity. At {@code 1.0g} it equals the configured offset exactly.</li>
 *     <li><b>Heaviness</b> scales inversely ({@code 2 / (1 + gravityRatio)}), so it grounds
 *         you more strongly in low/zero gravity (up to {@code 2x} in vacuum) and pulls more
 *         gently when gravity is already high. At {@code 1.0g} it equals the configured pull
 *         exactly.</li>
 * </ul>
 */
public final class PotionGravityMath {

    private PotionGravityMath() {}

    /** Upward offset added by Weightlessness (always {@code >= 0}, never exceeds {@code currentEffectiveGravity}). */
    public static double weightlessnessOffset(
            double perLevelOffset,
            int amplifier,
            double baseGravity,
            double currentEffectiveGravity) {
        if (!Double.isFinite(perLevelOffset) || !Double.isFinite(baseGravity) || !Double.isFinite(currentEffectiveGravity)) {
            return 0.0;
        }
        if (perLevelOffset <= 0.0 || baseGravity <= 0.0 || currentEffectiveGravity <= 0.0) {
            return 0.0;
        }
        int level = Math.max(0, amplifier) + 1;
        double fraction = Math.min(1.0, (perLevelOffset * level) / baseGravity);
        return currentEffectiveGravity * fraction;
    }

    /** Downward pull (positive magnitude, subtracted from offset) added by Heaviness. */
    public static double heavinessPull(
            double perLevelPull,
            int amplifier,
            double baseGravity,
            double currentEffectiveGravity) {
        if (!Double.isFinite(perLevelPull) || !Double.isFinite(baseGravity) || !Double.isFinite(currentEffectiveGravity)) {
            return 0.0;
        }
        if (perLevelPull <= 0.0 || baseGravity <= 0.0) {
            return 0.0;
        }
        int level = Math.max(0, amplifier) + 1;
        double gravityRatio = Math.max(0.0, currentEffectiveGravity / baseGravity);
        double scale = 2.0 / (1.0 + gravityRatio);
        return perLevelPull * level * scale;
    }
}
