package dinner.dev.endless_gravity.api;

/**
 * The kind of entity a gravity calculation is for. Endless Gravity resolves the base
 * offset differently per channel (players, mobs and each projectile/faling-block type
 * have their own config values).
 */
public enum GravityChannel {
    PLAYER,
    MOB,
    ITEM,
    ARROW,
    THROWN,
    BLOCK
}
