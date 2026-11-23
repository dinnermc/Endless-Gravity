package dinner.dev.endless_gravity;

import com.google.gson.JsonParser;
import dinner.dev.endless_gravity.api.BiomeGravityRegistry;
import dinner.dev.endless_gravity.api.BiomeGravityRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BiomeGravityRuleTest {

    @AfterEach
    void cleanup() {
        BiomeGravityRegistry.setDatapackRules(Map.of());
        BiomeGravityRegistry.clearSyncedRules();
    }

    @Test
    void parsesJsonAndComputesOffsets() {
        var obj = JsonParser.parseString("""
                {
                  "biome": "minecraft:windswept_hills",
                  "dimensions": ["minecraft:overworld"],
                  "gravity_multiplier": 0.25,
                  "priority": 10
                }
                """).getAsJsonObject();
        BiomeGravityRule rule = BiomeGravityRule.fromJson("test:hills", obj);
        assertEquals("test:hills", rule.id());
        assertFalse(rule.isTag());
        assertEquals(0.06, rule.resolveOffset(0.08), 1e-6);
        assertEquals(0.03, rule.resolveOffset(0.04), 1e-6);
        assertTrue(rule.matches("minecraft:overworld", "minecraft:windswept_hills", t -> false));
        assertFalse(rule.matches("minecraft:the_end", "minecraft:windswept_hills", t -> false));

        BiomeGravityRule decoded = BiomeGravityRule.decode(rule.encode());
        assertEquals(rule.id(), decoded.id());
        assertEquals(rule.target(), decoded.target());
        assertEquals(rule.priority(), decoded.priority());
        assertEquals(0.06, decoded.resolveOffset(0.08), 1e-6);
    }

    @Test
    void exactBiomeBeatsTagAtSamePriorityAndHigherPriorityWins() {
        BiomeGravityRule tagRule = BiomeGravityRule.byMultiplier("tag", "#c:is_mountain", null, 0.5, 5);
        BiomeGravityRule exactRule = BiomeGravityRule.byMultiplier("exact", "minecraft:jagged_peaks", null, 0.2, 5);
        BiomeGravityRule highTag = BiomeGravityRule.byMultiplier("high", "#c:is_special", null, 0.1, 20);
        BiomeGravityRegistry.setDatapackRules(Map.of(
                tagRule.id(), tagRule,
                exactRule.id(), exactRule,
                highTag.id(), highTag
        ));

        Set<String> tags = Set.of("c:is_mountain");
        BiomeGravityRule matched = BiomeGravityRegistry.match("minecraft:overworld", "minecraft:jagged_peaks", tags::contains);
        assertNotNull(matched);
        assertEquals("exact", matched.id());

        Set<String> specialTags = Set.of("c:is_mountain", "c:is_special");
        BiomeGravityRule matchedHigh = BiomeGravityRegistry.match("minecraft:overworld", "minecraft:jagged_peaks", specialTags::contains);
        assertNotNull(matchedHigh);
        assertEquals("high", matchedHigh.id());
    }
}
