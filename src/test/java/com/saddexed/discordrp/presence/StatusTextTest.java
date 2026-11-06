package com.saddexed.discordrp.presence;

import com.saddexed.discordrp.presence.StatusText.Kind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatusTextTest {
    private static final String CORVUS = "Corvus Star System";

    @Test
    void modCount() {
        assertEquals("Vanilla", StatusText.mods(0));
        assertEquals("1 Mod Loaded", StatusText.mods(1));
        assertEquals("23 Mods Loaded", StatusText.mods(23));
    }

    @Test
    void fullLines() {
        assertEquals("Fighting against Hegemony Inspection Fleet",
                StatusText.describe(Kind.FIGHTING, "Hegemony Inspection Fleet", CORVUS, true, true, true));
        assertEquals("Fleeing from Luddic Path Fleet",
                StatusText.describe(Kind.FLEEING, "Luddic Path Fleet", CORVUS, true, true, true));
        assertEquals("Interacting with Hegemony Fast Picket (friendly)",
                StatusText.describe(Kind.INTERACTING, "Hegemony Fast Picket (friendly)", CORVUS, true, true, true));
        assertEquals("Pursued by Hegemony Fast Picket",
                StatusText.describe(Kind.PURSUED, "Hegemony Fast Picket", CORVUS, true, true, true));
        assertEquals("Trading at Jangala",
                StatusText.describe(Kind.TRADING, "Jangala", CORVUS, true, true, true));
        assertEquals("Refitting at Jangala",
                StatusText.describe(Kind.REFITTING, "Jangala", CORVUS, true, true, true));
        assertEquals("Docked at Jangala",
                StatusText.describe(Kind.DOCKED, "Jangala", CORVUS, true, true, true));
        assertEquals("Travelling through Hyperspace",
                StatusText.describe(Kind.TRAVELLING, null, "Hyperspace", false, true, true));
        assertEquals("Exploring the Corvus Star System",
                StatusText.describe(Kind.EXPLORING, null, CORVUS, true, true, true));
    }

    @Test
    void combatLines() {
        assertEquals("Pursuing Pirate Raiders",
                StatusText.describe(Kind.PURSUING, "Pirate Raiders", null, false, true, true));
        assertEquals("In the Simulator", StatusText.describe(Kind.SIMULATOR, null, null, false, true, true));
        assertEquals("Playing a mission", StatusText.describe(Kind.MISSION, null, null, false, true, true));
        assertNull(StatusText.describe(Kind.FIGHTING, "Pirate Raiders", null, false, false, true));
    }

    @Test
    void locationHidden() {
        assertEquals("Fighting against Pirate Raiders",
                StatusText.describe(Kind.FIGHTING, "Pirate Raiders", CORVUS, true, true, false));
        assertEquals("Fleeing from Luddic Path Fleet",
                StatusText.describe(Kind.FLEEING, "Luddic Path Fleet", CORVUS, true, true, false));
        assertEquals("Trading", StatusText.describe(Kind.TRADING, "Jangala", CORVUS, true, true, false));
        assertEquals("Refitting", StatusText.describe(Kind.REFITTING, "Jangala", CORVUS, true, true, false));
        assertEquals("Docked", StatusText.describe(Kind.DOCKED, "Jangala", CORVUS, true, true, false));
        assertEquals("Travelling", StatusText.describe(Kind.TRAVELLING, null, "Hyperspace", false, true, false));
        assertEquals("Exploring", StatusText.describe(Kind.EXPLORING, null, CORVUS, true, true, false));
    }

    @Test
    void activityHidden() {
        assertEquals(CORVUS, StatusText.describe(Kind.FIGHTING, "Pirate Raiders", CORVUS, true, false, true));
        assertEquals("Hyperspace", StatusText.describe(Kind.TRAVELLING, null, "Hyperspace", false, false, true));
    }

    @Test
    void marketWithSystem() {
        assertEquals("Jangala (Corvus Star System)", StatusText.marketWithSystem("Jangala", CORVUS));
        assertEquals("Jangala", StatusText.marketWithSystem("Jangala", null));
        assertEquals("Trading at Jangala (Corvus Star System)", StatusText.describe(Kind.TRADING,
                StatusText.marketWithSystem("Jangala", CORVUS), CORVUS, true, true, true));
    }

    @Test
    void bothHidden() {
        assertNull(StatusText.describe(Kind.EXPLORING, null, CORVUS, true, false, false));
    }

    @Test
    void missingSubjectOrPlace() {
        assertEquals("Fighting", StatusText.describe(Kind.FIGHTING, null, CORVUS, true, true, true));
        assertEquals("Docked", StatusText.describe(Kind.DOCKED, null, CORVUS, true, true, true));
        assertEquals("Exploring", StatusText.describe(Kind.EXPLORING, null, null, false, true, true));
    }
}
