package dinner.dev.endless_gravity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtmospherePresetTest {

    @Test
    void exportAndImportRoundTrip() {
        AtmospherePreset preset = new AtmospherePreset("Lunar", 0.075, 319, null,
                0.1, 0.05, List.of("-64:1.0", "6000:0.0"));

        assertEquals(List.of(preset), AtmospherePreset.parseImport(
                AtmospherePreset.exportJson(List.of(preset))));
    }

    @Test
    void rejectsUnorderedLayers() {
        assertThrows(IllegalArgumentException.class, () -> new AtmospherePreset("Invalid",
                0.075, 319, null, 0.1, 0.05, List.of("100:1.0", "50:0.0")));
    }

    @Test
    void emptyLibraryCanBeExportedAndLoaded() {
        assertEquals(List.of(), AtmospherePreset.parseImport(AtmospherePreset.exportJson(List.of())));
    }
}
