package com.saddexed.discordrp.presence;

public final class PresenceLine {
    static final int MAX_LENGTH = 128;
    public static final String SEPARATOR = " · ";

    private PresenceLine() {}

    public static String join(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(part.trim());
        }
        if (sb.length() == 0) return null;
        return sb.length() > MAX_LENGTH ? sb.substring(0, MAX_LENGTH) : sb.toString();
    }

    // Some factions name fleets in lower case for mid-sentence use ("pirate Raiders")
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static String percent(float fraction) {
        float clamped = Math.max(0f, Math.min(1f, fraction));
        return Math.round(clamped * 100f) + "%";
    }

    public static String shipStats(float hull, float armour, float cr) {
        return join(SEPARATOR, "Hull " + percent(hull), "Armour " + percent(armour), percent(cr) + " CR");
    }

    public static String flagship(String shipName, String hullName) {
        boolean hasName = shipName != null && !shipName.trim().isEmpty();
        boolean hasHull = hullName != null && !hullName.trim().isEmpty();
        if (hasName && hasHull) return shipName.trim() + " (" + hullName.trim() + ")";
        if (hasName) return shipName.trim();
        if (hasHull) return hullName.trim();
        return null;
    }
}
