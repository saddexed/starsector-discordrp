package com.saddexed.discordrp.presence;

import com.fs.starfarer.api.Global;
import com.saddexed.discordrp.DiscordRp;
import com.saddexed.discordrp.ipc.Presence;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class PresenceSettings {
    private static final Logger log = Logger.getLogger(PresenceSettings.class);

    static final String SETTINGS_FILE = "data/config/discordrp_config.json";
    private static final String COMMON_FILE = "discordrp_settings.json";

    public static final String SHOW_ACTIVITY = "drp_showActivity";
    public static final String SHOW_LOCATION = "drp_showLocation";
    public static final String SHOW_INFO = "drp_showInfo";
    // Info line shows either IDENTITY or STATS: both together get cut off by Discord
    public static final String INFO_STATS = "drp_infoStats";
    public static final String SHOW_TITLE = "drp_showTitle";
    public static final String SHOW_NAME = "drp_showName";
    public static final String SHOW_LEVEL = "drp_showLevel";
    public static final String SHOW_CREDITS = "drp_showCredits";
    public static final String SHOW_AVERAGE_CR = "drp_showAverageCR";
    public static final String SHOW_SHIP_NAME = "drp_showShipName";
    public static final String SHOW_SHIP_HULL = "drp_showShipHull";
    public static final String SHOW_FACTION = "drp_showFaction";

    public static final String[][] STATUS_LINE = {
            {SHOW_ACTIVITY, "Activity"},
            {SHOW_LOCATION, "Location"},
    };
    // Info line, split in two menus
    public static final String[][] IDENTITY = {
            {SHOW_TITLE, "Title"},
            {SHOW_NAME, "Name"},
            {SHOW_SHIP_NAME, "Flagship Title"},
            {SHOW_SHIP_HULL, "Flagship Class"},
    };
    public static final String[][] STATS = {
            {SHOW_AVERAGE_CR, "Average CR"},
            {SHOW_CREDITS, "Credits"},
            {SHOW_LEVEL, "Level"},
            {SHOW_FACTION, "Faction"},
    };

    private static final Set<String> OFF_BY_DEFAULT =
            new HashSet<>(Arrays.asList(INFO_STATS, SHOW_TITLE, SHOW_NAME, SHOW_AVERAGE_CR, SHOW_FACTION));

    public static final String MENU = "drp_menuText";
    private static final String DEFAULT_MENU = "Stargazing the Main Menu";

    public static final String SETTINGS_KEY = "drp_settingsKey";
    private static final String DEFAULT_SETTINGS_KEY = "F8";

    private static JSONObject defaults;
    private static JSONObject saved;
    private static Integer settingsKey;

    private PresenceSettings() {}

    public static int settingsKey() {
        if (settingsKey == null) {
            String name = defaults().optString(SETTINGS_KEY, DEFAULT_SETTINGS_KEY).trim().toUpperCase();
            int code = Keyboard.getKeyIndex(name);
            if (code == Keyboard.KEY_NONE) {
                log.warn("[DiscordRP] Unknown " + SETTINGS_KEY + " \"" + name + "\", using "
                        + DEFAULT_SETTINGS_KEY);
                code = Keyboard.getKeyIndex(DEFAULT_SETTINGS_KEY);
            }
            settingsKey = code;
        }
        return settingsKey;
    }

    public static String applicationId() {
        String id = defaults().optString("applicationId", "").trim();
        return id.matches("\\d+") ? id : null;
    }

    public static Presence.Art art() {
        JSONObject json = defaults();
        return new Presence.Art(json.optString("largeImage", null), json.optString("largeText", null),
                json.optString("smallImage", null), json.optString("smallText", null));
    }

    public static String menu() {
        String value = defaults().optString(MENU, null);
        return value == null || value.trim().isEmpty() ? DEFAULT_MENU : value.trim();
    }

    public static boolean show(String key) {
        if (saved().has(key)) return saved().optBoolean(key);
        return defaults().optBoolean(key, !OFF_BY_DEFAULT.contains(key));
    }

    private static final Map<String, String> REQUIRES = new HashMap<>();
    static {
        REQUIRES.put(SHOW_TITLE, SHOW_NAME);
        REQUIRES.put(SHOW_SHIP_HULL, SHOW_SHIP_NAME);
    }

    public static String requires(String key) {
        return REQUIRES.get(key);
    }

    public static boolean isShown(String key) {
        return isShown(key, PresenceSettings::show);
    }

    static boolean isShown(String key, Predicate<String> ticked) {
        String needed = REQUIRES.get(key);
        return ticked.test(key) && (needed == null || isShown(needed, ticked));
    }

    public static String label(String key) {
        for (String[][] items : new String[][][] {IDENTITY, STATS, STATUS_LINE}) {
            for (String[] item : items) {
                if (item[0].equals(key)) return item[1];
            }
        }
        return key;
    }

    public static void setShow(String key, boolean value) {
        try {
            saved().put(key, value);
            Global.getSettings().writeJSONToCommon(COMMON_FILE, saved(), false);
        } catch (Exception e) {
            log.warn("[DiscordRP] Could not save setting " + key + ": " + e.getMessage());
        }
    }

    private static JSONObject defaults() {
        if (defaults == null) {
            try {
                defaults = Global.getSettings().loadJSON(SETTINGS_FILE, DiscordRp.MOD_ID);
            } catch (Exception e) {
                log.warn("[DiscordRP] Could not read " + SETTINGS_FILE + ": " + e.getMessage());
                defaults = new JSONObject();
            }
        }
        return defaults;
    }

    private static JSONObject saved() {
        if (saved == null) {
            try {
                saved = Global.getSettings().fileExistsInCommon(COMMON_FILE)
                        ? Global.getSettings().readJSONFromCommon(COMMON_FILE, false)
                        : new JSONObject();
            } catch (Exception e) {
                log.warn("[DiscordRP] Could not read saved settings: " + e.getMessage());
                saved = new JSONObject();
            }
        }
        return saved;
    }
}
