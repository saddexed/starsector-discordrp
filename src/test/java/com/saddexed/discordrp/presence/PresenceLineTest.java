package com.saddexed.discordrp.presence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PresenceLineTest {
    @Test
    void joinsAllParts() {
        assertEquals("Captain Alex | Lvl 8 | ISS Hammer (Onslaught)",
                PresenceLine.join(" | ", "Captain Alex", "Lvl 8", "ISS Hammer (Onslaught)"));
    }

    @Test
    void skipsUntickedPartsWithoutDanglingSeparators() {
        assertEquals("Lvl 8", PresenceLine.join(" | ", null, "Lvl 8", ""));
        assertEquals("Corvus · 1,234 ¢", PresenceLine.join(" · ", "Corvus", null, "1,234 ¢"));
    }

    @Test
    void nothingTickedGivesNull() {
        assertNull(PresenceLine.join(" | ", null, " ", ""));
    }

    @Test
    void capsLength() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 200; i++) longText.append('x');
        assertEquals(PresenceLine.MAX_LENGTH, PresenceLine.join(" | ", longText.toString()).length());
    }

    @Test
    void capitalizesFirstLetterOnly() {
        assertEquals("Pirate Raiders", PresenceLine.capitalize("pirate Raiders"));
        assertEquals("Hegemony Inspection Fleet", PresenceLine.capitalize("Hegemony Inspection Fleet"));
        assertEquals("", PresenceLine.capitalize(""));
        assertNull(PresenceLine.capitalize(null));
    }

    @Test
    void percentRoundsAndClamps() {
        assertEquals("45%", PresenceLine.percent(0.45f));
        assertEquals("46%", PresenceLine.percent(0.456f));
        assertEquals("100%", PresenceLine.percent(1.7f));
        assertEquals("0%", PresenceLine.percent(-0.2f));
    }

    @Test
    void shipStats() {
        assertEquals("Hull 72% \u00B7 Armour 64% \u00B7 68% CR", PresenceLine.shipStats(0.72f, 0.64f, 0.68f));
    }

    @Test
    void flagshipCombinations() {
        assertEquals("ISS Hammer (Onslaught)", PresenceLine.flagship("ISS Hammer", "Onslaught"));
        assertEquals("ISS Hammer", PresenceLine.flagship("ISS Hammer", null));
        assertEquals("Onslaught", PresenceLine.flagship(null, "Onslaught"));
        assertNull(PresenceLine.flagship(null, null));
    }
}
