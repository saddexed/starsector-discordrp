package com.saddexed.discordrp.presence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PresenceTextTest {
    @Test
    void unnamedFactionIsHidden() {
        assertNull(PresenceText.factionName("Your"));
        assertNull(PresenceText.factionName(null));
        assertEquals("Iron Dominion", PresenceText.factionName("Iron Dominion"));
    }

    @Test
    void formatsCreditsWithCommas() {
        assertEquals("0", PresenceText.formatCredits(0));
        assertEquals("999", PresenceText.formatCredits(999));
        assertEquals("1,000", PresenceText.formatCredits(1000));
        assertEquals("12,345", PresenceText.formatCredits(12345));
        assertEquals("1,234,567", PresenceText.formatCredits(1234567));
    }
}
