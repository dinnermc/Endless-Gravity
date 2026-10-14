package dinner.dev.endless_gravity.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backing store for {@link GravityApi}'s modifier registration.
 *
 * <p>This class is public only so Endless Gravity's internal resolver can reach it.
 * Addons should use {@link GravityApi} instead.
 */
public final class GravityModifierRegistry {

    /** A registered modifier and the priority it was registered with. */
    public record Entry(ResourceLocation id, int priority, GravityModifier modifier) {}

    private static final Map<ResourceLocation, Entry> MODIFIERS = new ConcurrentHashMap<>();
    private static final Map<Entity, GravityModifier> ENTITY_MODIFIERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Immutable, priority-sorted snapshot. Rebuilt on every registration change. */
    private static volatile List<Entry> sorted = List.of();

    private GravityModifierRegistry() {}

    public static synchronized void register(ResourceLocation id, int priority, GravityModifier modifier) {
        if (id == null || modifier == null) return;
        MODIFIERS.put(id, new Entry(id, priority, modifier));
        rebuild();
    }

    public static synchronized void unregister(ResourceLocation id) {
        if (MODIFIERS.remove(id) != null) {
            rebuild();
        }
    }

    public static void setEntityModifier(Entity entity, GravityModifier modifier) {
        if (entity == null) return;
        if (modifier == null) {
            ENTITY_MODIFIERS.remove(entity);
        } else {
            ENTITY_MODIFIERS.put(entity, modifier);
        }
    }

    public static void clearEntityModifier(Entity entity) {
        if (entity != null) ENTITY_MODIFIERS.remove(entity);
    }

    public static GravityModifier getEntityModifier(Entity entity) {
        return entity == null ? null : ENTITY_MODIFIERS.get(entity);
    }

    /** Run every modifier against the context, highest priority first. */
    public static void apply(GravityContext context) {
        for (Entry entry : sorted) {
            entry.modifier().modify(context);
        }
        GravityModifier perEntity = ENTITY_MODIFIERS.get(context.entity());
        if (perEntity != null) {
            perEntity.modify(context);
        }
    }

    private static void rebuild() {
        List<Entry> list = new ArrayList<>(MODIFIERS.values());
        list.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        sorted = Collections.unmodifiableList(list);
    }
}
