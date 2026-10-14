package dinner.dev.endless_gravity.api;

/**
 * Where the base offset for a calculation came from, before any {@link GravityModifier} ran.
 */
public enum GravitySource {
    /** No base offset applies (vanilla gravity is left untouched). */
    NONE,
    /** Reduced gravity in The End. */
    END,
    /** Altitude-based atmosphere gravity in the Overworld (and Sable sub-levels). */
    ATMOSPHERE,
    /** Per-dimension gravity from {@code config/endless_gravity/dimensions.json}. */
    CUSTOM_DIMENSION
}
