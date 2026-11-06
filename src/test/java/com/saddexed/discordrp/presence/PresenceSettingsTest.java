package com.saddexed.discordrp.presence;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceSettingsTest {
    private static boolean shown(String key, String... ticked) {
        Set<String> set = new HashSet<>(Arrays.asList(ticked));
        return PresenceSettings.isShown(key, set::contains);
    }

    @Test
    void titleNeedsName() {
        assertEquals(PresenceSettings.SHOW_NAME, PresenceSettings.requires(PresenceSettings.SHOW_TITLE));
        assertTrue(shown(PresenceSettings.SHOW_TITLE, PresenceSettings.SHOW_TITLE, PresenceSettings.SHOW_NAME));
        assertFalse(shown(PresenceSettings.SHOW_TITLE, PresenceSettings.SHOW_TITLE));
    }

    @Test
    void flagshipClassNeedsFlagshipName() {
        assertEquals(PresenceSettings.SHOW_SHIP_NAME, PresenceSettings.requires(PresenceSettings.SHOW_SHIP_HULL));
        assertTrue(shown(PresenceSettings.SHOW_SHIP_HULL, PresenceSettings.SHOW_SHIP_HULL, PresenceSettings.SHOW_SHIP_NAME));
        assertFalse(shown(PresenceSettings.SHOW_SHIP_HULL, PresenceSettings.SHOW_SHIP_HULL));
    }

    @Test
    void untickedItemIsHidden() {
        assertFalse(shown(PresenceSettings.SHOW_TITLE, PresenceSettings.SHOW_NAME));
        assertFalse(shown(PresenceSettings.SHOW_CREDITS));
    }

    @Test
    void independentItemsOnlyNeedThemselves() {
        assertNull(PresenceSettings.requires(PresenceSettings.SHOW_NAME));
        assertTrue(shown(PresenceSettings.SHOW_NAME, PresenceSettings.SHOW_NAME));
        assertTrue(shown(PresenceSettings.SHOW_CREDITS, PresenceSettings.SHOW_CREDITS));
    }

    @Test
    void labels() {
        assertEquals("Name", PresenceSettings.label(PresenceSettings.SHOW_NAME));
        assertEquals("Faction", PresenceSettings.label(PresenceSettings.SHOW_FACTION));
        assertEquals("Average CR", PresenceSettings.label(PresenceSettings.SHOW_AVERAGE_CR));
    }
}
