package dinner.dev.endless_gravity.api;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backing store for {@link GravityApi}'s modifier registration.
 *
 * <p>This class is public only so Endless Gravity's internal resolver can reach it.
 * Addons should use {@link GravityApi} instead.
 */
public final class GravityModifierRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** A registered modifier and the priority it was registered with. */
    public record Entry(ResourceLocation id, int priority, GravityModifier modifier) {}

    /**
     * Weak key using reference identity ({@code ==}) instead of {@link Entity#equals},
     * because {@code Entity.equals} compares only numeric entity IDs and collides between
     * ClientLevel and ServerLevel in singleplayer or across dimensions.
     */
    private static final class IdentityWeakKey extends WeakReference<Entity> {
        private final int identityHash;

        IdentityWeakKey(Entity referent, ReferenceQueue<Entity> queue) {
            super(referent, queue);
            this.identityHash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return identityHash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IdentityWeakKey other)) return false;
            Entity a = this.get();
            Entity b = other.get();
            return a != null && a == b;
        }
    }

    private static final Comparator<Entry> ORDER = Comparator
            .comparingInt(Entry::priority).reversed()
            .thenComparing(Entry::id);

    private static final Map<ResourceLocation, Entry> MODIFIERS = new ConcurrentHashMap<>();
    private static final ReferenceQueue<Entity> ENTITY_QUEUE = new ReferenceQueue<>();
    private static final ConcurrentHashMap<IdentityWeakKey, GravityModifier> ENTITY_MODIFIERS =
            new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> FAILED_MODIFIERS = ConcurrentHashMap.newKeySet();

    /** Immutable, deterministically priority-sorted snapshot. Rebuilt on every registration change. */
    private static volatile List<Entry> sorted = List.of();
    /** Fast-path flag to skip entity-modifier map lookups on the hot tick path when unused. */
    private static volatile boolean hasEntityModifiers = false;

    private GravityModifierRegistry() {}

    public static synchronized void register(ResourceLocation id, int priority, GravityModifier modifier) {
        if (id == null || modifier == null) return;
        MODIFIERS.put(id, new Entry(id, priority, modifier));
        FAILED_MODIFIERS.remove(id);
        rebuild();
    }

    public static synchronized void unregister(ResourceLocation id) {
        if (id == null) return;
        FAILED_MODIFIERS.remove(id);
        if (MODIFIERS.remove(id) != null) {
            rebuild();
        }
    }

    /** Immutable snapshot of all registered global modifiers in execution order. */
    public static List<Entry> getRegisteredModifiers() {
        return sorted;
    }

    public static void setEntityModifier(Entity entity, GravityModifier modifier) {
        if (entity == null) return;
        expungeStaleEntries();
        if (modifier == null) {
            ENTITY_MODIFIERS.remove(new IdentityWeakKey(entity, null));
            hasEntityModifiers = !ENTITY_MODIFIERS.isEmpty();
        } else {
            ENTITY_MODIFIERS.put(new IdentityWeakKey(entity, ENTITY_QUEUE), modifier);
            hasEntityModifiers = true;
        }
    }

    public static void clearEntityModifier(Entity entity) {
        if (entity == null) return;
        expungeStaleEntries();
        if (ENTITY_MODIFIERS.remove(new IdentityWeakKey(entity, null)) != null) {
            hasEntityModifiers = !ENTITY_MODIFIERS.isEmpty();
        }
    }

    public static GravityModifier getEntityModifier(Entity entity) {
        if (entity == null || !hasEntityModifiers) return null;
        expungeStaleEntries();
        return ENTITY_MODIFIERS.get(new IdentityWeakKey(entity, null));
    }

    /** Run every modifier against the context, highest priority first. */
    public static void apply(GravityContext context) {
        List<Entry> snapshot = sorted;
        for (int i = 0, size = snapshot.size(); i < size; i++) {
            Entry entry = snapshot.get(i);
            try {
                entry.modifier().modify(context);
            } catch (RuntimeException e) {
                if (FAILED_MODIFIERS.add(entry.id())) {
                    LOGGER.error("GravityModifier '{}' threw an exception (further errors suppressed)",
                            entry.id(), e);
                }
            }
        }
        if (hasEntityModifiers) {
            Entity entity = context.entity();
            if (entity != null) {
                GravityModifier perEntity = ENTITY_MODIFIERS.get(new IdentityWeakKey(entity, null));
                if (perEntity != null) {
                    try {
                        perEntity.modify(context);
                    } catch (RuntimeException e) {
                        LOGGER.error("Per-entity GravityModifier for {} threw an exception", entity, e);
                    }
                }
            }
        }
    }

    private static void expungeStaleEntries() {
        IdentityWeakKey ref;
        boolean removed = false;
        while ((ref = (IdentityWeakKey) ENTITY_QUEUE.poll()) != null) {
            ENTITY_MODIFIERS.remove(ref);
            removed = true;
        }
        if (removed && ENTITY_MODIFIERS.isEmpty()) {
            hasEntityModifiers = false;
        }
    }

    private static void rebuild() {
        List<Entry> list = new ArrayList<>(MODIFIERS.values());
        list.sort(ORDER);
        sorted = Collections.unmodifiableList(list);
    }
}
