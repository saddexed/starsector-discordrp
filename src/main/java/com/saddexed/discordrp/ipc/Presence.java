package com.saddexed.discordrp.ipc;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public final class Presence {
    // Discord rejects the whole activity if details/state is shorter or longer
    static final int MIN_LENGTH = 2;
    static final int MAX_LENGTH = 128;

    public final String details;
    public final String state;
    public final long startMillis;
    public final Art art;

    public Presence(String details, String state, long startMillis, Art art) {
        this.details = clean(details);
        this.state = clean(state);
        this.startMillis = startMillis;
        this.art = art == null ? Art.NONE : art;
    }

    public static final class Art {
        public static final Art NONE = new Art(null, null, null, null);

        final String largeImage, largeText, smallImage, smallText;

        public Art(String largeImage, String largeText, String smallImage, String smallText) {
            this.largeImage = blankToNull(largeImage);
            this.largeText = clean(largeText);
            this.smallImage = blankToNull(smallImage);
            this.smallText = clean(smallText);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Art)) return false;
            Art a = (Art) o;
            return Objects.equals(largeImage, a.largeImage) && Objects.equals(largeText, a.largeText)
                    && Objects.equals(smallImage, a.smallImage) && Objects.equals(smallText, a.smallText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(largeImage, largeText, smallImage, smallText);
        }
    }

    public JSONObject toJson() {
        try {
            JSONObject activity = new JSONObject();
            if (details != null) activity.put("details", details);
            if (state != null) activity.put("state", state);
            if (startMillis > 0) activity.put("timestamps", new JSONObject().put("start", startMillis));

            JSONObject assets = new JSONObject();
            if (art.largeImage != null) {
                assets.put("large_image", art.largeImage);
                if (art.largeText != null) assets.put("large_text", art.largeText);
            }
            if (art.smallImage != null) {
                assets.put("small_image", art.smallImage);
                if (art.smallText != null) assets.put("small_text", art.smallText);
            }
            if (assets.length() > 0) activity.put("assets", assets);
            return activity;
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    static String clean(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.length() > MAX_LENGTH) trimmed = trimmed.substring(0, MAX_LENGTH).trim();
        return trimmed.length() < MIN_LENGTH ? null : trimmed;
    }

    private static String blankToNull(String text) {
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Presence)) return false;
        Presence p = (Presence) o;
        return Objects.equals(details, p.details) && Objects.equals(state, p.state)
                && startMillis == p.startMillis && art.equals(p.art);
    }

    @Override
    public int hashCode() {
        return Objects.hash(details, state, startMillis, art);
    }

    @Override
    public String toString() {
        return "[" + details + " / " + state + "]";
    }
}
