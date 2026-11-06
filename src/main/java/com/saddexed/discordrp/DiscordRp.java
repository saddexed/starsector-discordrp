package com.saddexed.discordrp;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.saddexed.discordrp.ipc.Presence;
import com.saddexed.discordrp.ipc.PresenceService;
import com.saddexed.discordrp.ipc.Transport;
import com.saddexed.discordrp.presence.PresenceSettings;
import com.saddexed.discordrp.presence.StatusText;
import org.apache.log4j.Logger;

public final class DiscordRp {
    private static final Logger log = Logger.getLogger(DiscordRp.class);

    public static final String MOD_ID = "saddexed_discordrp";

    private static PresenceService service;
    private static Presence.Art art = Presence.Art.NONE;
    private static long timerStart = System.currentTimeMillis();
    private static boolean inMenu;

    private DiscordRp() {}

    static void start() {
        if (service != null) return;
        String appId = PresenceSettings.applicationId();
        if (appId == null) {
            log.warn("[DiscordRP] No applicationId in data/config/discordrp_config.json, presence disabled.");
            return;
        }
        log.info("[DiscordRP] Starting "
                + Global.getSettings().getModManager().getModSpec(MOD_ID).getVersion() + ".");
        art = PresenceSettings.art();
        service = new PresenceService(Transport.forOs(System.getProperty("os.name")), appId,
                System::currentTimeMillis);
        service.start();
    }

    public static void show(String details, String state) {
        inMenu = false;
        send(details, state);
    }

    public static void showMenu() {
        if (!inMenu) {
            inMenu = true;
            restartTimer();
        }
        send(StatusText.mods(otherMods()), PresenceSettings.menu());
    }

    private static int otherMods() {
        int count = 0;
        for (ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            if (!MOD_ID.equals(mod.getId())) count++;
        }
        return count;
    }

    public static void restartTimer() {
        timerStart = System.currentTimeMillis();
    }

    private static void send(String details, String state) {
        if (service != null) service.update(new Presence(details, state, timerStart, art));
    }
}
