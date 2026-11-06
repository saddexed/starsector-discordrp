package com.saddexed.discordrp.presence;

public final class StatusText {
    public enum Kind {
        FIGHTING, INTERACTING, FLEEING, PURSUED, PURSUING, TRADING, REFITTING, DOCKED, TRAVELLING, EXPLORING,
        SIMULATOR, MISSION
    }

    private StatusText() {}

    /** Menu card's top line; {@code otherMods} excludes this mod. */
    public static String mods(int otherMods) {
        if (otherMods <= 0) return "Vanilla";
        return otherMods + (otherMods == 1 ? " Mod Loaded" : " Mods Loaded");
    }

    public static String describe(Kind kind, String subject, String place, boolean article,
                                  boolean showActivity, boolean showLocation) {
        String where = showLocation && place != null ? (article ? "the " + place : place) : null;

        if (!showActivity) {
            return showLocation ? PresenceLine.join(" ", place) : null;
        }

        switch (kind) {
            case FIGHTING:
                return PresenceLine.join(" ", "Fighting", subject == null ? null : "against " + subject);
            case INTERACTING:
                return PresenceLine.join(" ", "Interacting", subject == null ? null : "with " + subject);
            case FLEEING:
                return PresenceLine.join(" ", "Fleeing", subject == null ? null : "from " + subject);
            case PURSUED:
                return PresenceLine.join(" ", "Pursued", subject == null ? null : "by " + subject);
            case PURSUING:
                return PresenceLine.join(" ", "Pursuing", subject);
            case SIMULATOR:
                return "In the Simulator";
            case MISSION:
                return "Playing a mission";
            case TRADING:
                return atMarket("Trading", subject, showLocation);
            case REFITTING:
                return atMarket("Refitting", subject, showLocation);
            case DOCKED:
                return atMarket("Docked", subject, showLocation);
            case TRAVELLING:
                return PresenceLine.join(" ", "Travelling", where == null ? null : "through " + where);
            case EXPLORING:
            default:
                return PresenceLine.join(" ", "Exploring", where);
        }
    }

    public static String marketWithSystem(String market, String system) {
        if (market == null) return null;
        return system == null || system.trim().isEmpty() ? market : market + " (" + system.trim() + ")";
    }

    // The market name gives the location away, so it follows the Location checkbox
    private static String atMarket(String verb, String market, boolean showLocation) {
        return PresenceLine.join(" ", verb, showLocation && market != null ? "at " + market : null);
    }
}
