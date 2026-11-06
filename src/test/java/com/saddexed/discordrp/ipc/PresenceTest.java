package com.saddexed.discordrp.ipc;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PresenceTest {
    private static final Presence.Art ART = new Presence.Art("logo", "Starsector", " ", "ignored");

    @Test
    void cleansText() {
        assertEquals("Lv 8", Presence.clean("  Lv 8 "));
        assertNull(Presence.clean("x"));
        assertNull(Presence.clean("   "));
        assertNull(Presence.clean(null));
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 200; i++) longText.append('x');
        assertEquals(Presence.MAX_LENGTH, Presence.clean(longText.toString()).length());
    }

    @Test
    void json() throws Exception {
        JSONObject json = new Presence("Lv 8", "Exploring", 1000L, ART).toJson();
        assertEquals("Lv 8", json.getString("details"));
        assertEquals("Exploring", json.getString("state"));
        assertEquals(1000L, json.getJSONObject("timestamps").getLong("start"));
        JSONObject assets = json.getJSONObject("assets");
        assertEquals("logo", assets.getString("large_image"));
        assertEquals("Starsector", assets.getString("large_text"));
        assertFalse(assets.has("small_image"));
        assertFalse(assets.has("small_text"));
    }

    @Test
    void emptyPartsAreLeftOut() {
        JSONObject json = new Presence(null, "x", 0, null).toJson();
        assertEquals(0, json.length());
    }

    @Test
    void equalByValue() {
        assertEquals(new Presence("Lv 8", null, 5, ART), new Presence(" Lv 8", "", 5, ART));
        assertNotEquals(new Presence("Lv 8", null, 5, ART), new Presence("Lv 9", null, 5, ART));
        assertNotEquals(new Presence("Lv 8", null, 5, ART), new Presence("Lv 8", null, 6, ART));
    }
}
