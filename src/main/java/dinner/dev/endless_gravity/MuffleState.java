package dinner.dev.endless_gravity;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.EXTEfx.*;

/**
 * Manages OpenAL EFX low-pass filtering for Endless Gravity.
 *
 * <p>All OpenAL calls are executed strictly on Minecraft's {@code SoundEngineExecutor}
 * audio thread. Supports three source roles:
 * <ul>
 *     <li>{@link ChannelRole#EXEMPT} — UI clicks, music, and relative master sounds (never muffled).</li>
 *     <li>{@link ChannelRole#SELF_SUIT} — local player's own sounds (warm helmet/suit conduction).</li>
 *     <li>{@link ChannelRole#WORLD} — external world sounds (full atmospheric/End low-pass muffle).</li>
 * </ul>
 */
public final class MuffleState {

    public enum ChannelRole {
        WORLD,
        SELF_SUIT,
        EXEMPT
    }

    private static int worldFilterId = -1;
    private static int selfFilterId = -1;
    private static Boolean efxSupported = null;

    // Smoothed state (audio thread writes; volatile applied fields safe for API reads)
    private static float smoothedGain = 1.0f;
    private static float smoothedGainHF = 1.0f;
    private static volatile float appliedGain = 1.0f;
    private static volatile float appliedGainHF = 1.0f;
    private static volatile boolean appliedActive = false;

    private static final ConcurrentHashMap<Integer, ChannelRole> ACTIVE_SOURCES = new ConcurrentHashMap<>();

    private MuffleState() {}

    /**
     * Steps smoothing toward {@code (targetGain, targetGainHF)} and updates OpenAL filters.
     * Must be invoked on the OpenAL {@code SoundEngineExecutor} thread.
     */
    public static void tickAudioThread(float targetGain, float targetGainHF) {
        if (!ensureEfxReady()) return;

        float tGain = MuffleMath.clamp01f(targetGain);
        float tGainHF = MuffleMath.clamp01f(targetGainHF);

        smoothedGain = MuffleMath.smooth(smoothedGain, tGain);
        smoothedGainHF = MuffleMath.smooth(smoothedGainHF, tGainHF);

        boolean shouldFilter = smoothedGain < 0.999f || smoothedGainHF < 0.999f;
        if (!shouldFilter && !appliedActive) {
            smoothedGain = 1.0f;
            smoothedGainHF = 1.0f;
            appliedGain = 1.0f;
            appliedGainHF = 1.0f;
            return;
        }

        boolean changed = !appliedActive && shouldFilter
                || appliedActive && !shouldFilter
                || Math.abs(smoothedGain - appliedGain) > 0.001f
                || Math.abs(smoothedGainHF - appliedGainHF) > 0.001f;

        if (!changed) return;

        if (shouldFilter) {
            if (!ensureFiltersAllocated()) return;
            alFilterf(worldFilterId, AL_LOWPASS_GAIN, smoothedGain);
            alFilterf(worldFilterId, AL_LOWPASS_GAINHF, smoothedGainHF);

            float selfGain = MuffleMath.selfSuitGain(smoothedGain);
            float selfGainHF = MuffleMath.selfSuitGainHF(smoothedGainHF);
            alFilterf(selfFilterId, AL_LOWPASS_GAIN, selfGain);
            alFilterf(selfFilterId, AL_LOWPASS_GAINHF, selfGainHF);

            appliedGain = smoothedGain;
            appliedGainHF = smoothedGainHF;
            appliedActive = true;
            reapplyAllActiveSources();
        } else {
            smoothedGain = 1.0f;
            smoothedGainHF = 1.0f;
            appliedGain = 1.0f;
            appliedGainHF = 1.0f;
            appliedActive = false;
            reapplyAllActiveSources();
        }
    }

    /** Immediate reset to unfiltered state (e.g., disconnecting back to main menu). Audio thread. */
    public static void resetAudioThread() {
        smoothedGain = 1.0f;
        smoothedGainHF = 1.0f;
        if (!appliedActive) {
            appliedGain = 1.0f;
            appliedGainHF = 1.0f;
            return;
        }
        appliedGain = 1.0f;
        appliedGainHF = 1.0f;
        appliedActive = false;
        if (Boolean.TRUE.equals(efxSupported)) {
            reapplyAllActiveSources();
        }
    }

    /** Called from {@code ChannelMixin#play} on the audio thread. */
    public static void onChannelPlay(int sourceId) {
        if (sourceId == 0) return;
        ChannelRole role = ACTIVE_SOURCES.computeIfAbsent(sourceId, k -> ChannelRole.WORLD);
        applyFilterToSource(sourceId, role);
    }

    /** Called from {@code SoundEngineMixin#play} via {@code ChannelHandle#execute} on the audio thread. */
    public static void classifySource(int sourceId, ChannelRole role) {
        if (sourceId == 0 || role == null) return;
        ACTIVE_SOURCES.put(sourceId, role);
        applyFilterToSource(sourceId, role);
    }

    /** Called from {@code ChannelMixin#stop} and {@code ChannelMixin#destroy} on the audio thread. */
    public static void unregisterSource(int sourceId) {
        if (sourceId != 0) {
            ACTIVE_SOURCES.remove(sourceId);
        }
    }

    /** Called from {@code SoundEngineMixin#destroy} when OpenAL context is torn down (e.g. F3+T). */
    public static void onAudioContextDestroyed() {
        ACTIVE_SOURCES.clear();
        worldFilterId = -1;
        selfFilterId = -1;
        efxSupported = null;
        smoothedGain = 1.0f;
        smoothedGainHF = 1.0f;
        appliedGain = 1.0f;
        appliedGainHF = 1.0f;
        appliedActive = false;
    }

    public static boolean isActive() {
        return appliedActive && (appliedGain < 0.999f || appliedGainHF < 0.999f);
    }

    /** Current applied OpenAL world low-pass overall gain in {@code [0.0, 1.0]}. */
    public static float getCurrentGain() {
        return appliedActive ? appliedGain : 1.0f;
    }

    /** Current applied OpenAL world low-pass high-frequency gain in {@code [0.0, 1.0]}. */
    public static float getCurrentGainHF() {
        return appliedActive ? appliedGainHF : 1.0f;
    }

    private static void reapplyAllActiveSources() {
        Iterator<Map.Entry<Integer, ChannelRole>> it = ACTIVE_SOURCES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ChannelRole> entry = it.next();
            int sourceId = entry.getKey();
            if (sourceId == 0 || !alIsSource(sourceId)) {
                it.remove();
                continue;
            }
            applyFilterToSource(sourceId, entry.getValue());
        }
    }

    private static void applyFilterToSource(int sourceId, ChannelRole role) {
        if (!Boolean.TRUE.equals(efxSupported) || sourceId == 0 || !alIsSource(sourceId)) {
            return;
        }
        int filter = AL_FILTER_NULL;
        if (appliedActive) {
            if (role == ChannelRole.WORLD && worldFilterId != -1 && alIsFilter(worldFilterId)) {
                filter = worldFilterId;
            } else if (role == ChannelRole.SELF_SUIT && selfFilterId != -1 && alIsFilter(selfFilterId)) {
                filter = selfFilterId;
            }
        }
        alSourcei(sourceId, AL_DIRECT_FILTER, filter);
    }

    private static boolean ensureEfxReady() {
        if (efxSupported != null) return efxSupported;
        try {
            alGetError(); // clear prior errors
            int probe = alGenFilters();
            int err = alGetError();
            if (err != AL_NO_ERROR || probe == 0 || !alIsFilter(probe)) {
                efxSupported = false;
                return false;
            }
            alFilteri(probe, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            err = alGetError();
            alDeleteFilters(probe);
            alGetError();
            efxSupported = (err == AL_NO_ERROR);
        } catch (Throwable t) {
            efxSupported = false;
        }
        return efxSupported;
    }

    private static boolean ensureFiltersAllocated() {
        if (worldFilterId == -1 || !alIsFilter(worldFilterId)) {
            worldFilterId = alGenFilters();
            if (worldFilterId == 0 || !alIsFilter(worldFilterId)) {
                worldFilterId = -1;
                return false;
            }
            alFilteri(worldFilterId, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        }
        if (selfFilterId == -1 || !alIsFilter(selfFilterId)) {
            selfFilterId = alGenFilters();
            if (selfFilterId == 0 || !alIsFilter(selfFilterId)) {
                selfFilterId = -1;
                return false;
            }
            alFilteri(selfFilterId, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        }
        return true;
    }
}
