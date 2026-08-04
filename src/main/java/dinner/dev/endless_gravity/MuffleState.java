package dinner.dev.endless_gravity;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.EXTEfx.*;

public class MuffleState {
    private static int filterId = -1;
    private static float currentGain = 1.0f;
    private static float currentGainHF = 1.0f;

    public static int getFilter() {
        if (filterId == -1) {
            filterId = alGenFilters();
            alFilteri(filterId, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        }
        return filterId;
    }

    public static void update(float gain, float gainHF) {
        if (gain != currentGain || gainHF != currentGainHF) {
            alFilterf(filterId, AL_LOWPASS_GAIN, gain);
            alFilterf(filterId, AL_LOWPASS_GAINHF, gainHF);
            currentGain = gain;
            currentGainHF = gainHF;
        }
    }

    public static boolean isActive() {
        return currentGain < 1.0f || currentGainHF < 1.0f;
    }
}
