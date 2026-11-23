package dinner.dev.endless_gravity.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of client-side {@link AudioMuffleModifier}s.
 * Higher priority runs first; reads use a lock-free volatile snapshot.
 */
public final class AudioMuffleRegistry {

    public record Entry(ResourceLocation id, int priority, AudioMuffleModifier modifier) {}

    private static final ConcurrentHashMap<ResourceLocation, Entry> MODIFIERS = new ConcurrentHashMap<>();
    private static volatile List<Entry> SNAPSHOT = List.of();

    private AudioMuffleRegistry() {}

    public static void register(ResourceLocation id, int priority, AudioMuffleModifier modifier) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (modifier == null) throw new IllegalArgumentException("modifier must not be null");
        MODIFIERS.put(id, new Entry(id, priority, modifier));
        rebuild();
    }

    public static void unregister(ResourceLocation id) {
        if (id != null && MODIFIERS.remove(id) != null) {
            rebuild();
        }
    }

    public static boolean hasModifiers() {
        return !SNAPSHOT.isEmpty();
    }

    public static List<Entry> entries() {
        return SNAPSHOT;
    }

    public static void apply(AudioMuffleContext context) {
        List<Entry> list = SNAPSHOT;
        for (int i = 0, n = list.size(); i < n; i++) {
            list.get(i).modifier().modify(context);
        }
    }

    private static void rebuild() {
        if (MODIFIERS.isEmpty()) {
            SNAPSHOT = List.of();
            return;
        }
        List<Entry> list = new ArrayList<>(MODIFIERS.values());
        list.sort(Comparator.comparingInt(Entry::priority).reversed()
                .thenComparing(e -> e.id().toString()));
        SNAPSHOT = List.copyOf(list);
    }
}
