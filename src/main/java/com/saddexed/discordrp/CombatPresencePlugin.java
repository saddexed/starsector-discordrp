package com.saddexed.discordrp;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.saddexed.discordrp.presence.PresenceText;
import org.apache.log4j.Logger;

import java.util.List;

public class CombatPresencePlugin extends BaseEveryFrameCombatPlugin {
    private static final Logger log = Logger.getLogger(CombatPresencePlugin.class);

    private static final float UPDATE_INTERVAL = 5f;

    private CombatEngineAPI engine;
    private long lastPush;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        long now = System.currentTimeMillis();
        if (lastPush != 0 && now - lastPush < UPDATE_INTERVAL * 1000) return;
        lastPush = now;

        try {
            // The main menu runs a background battle; that's our cue the player left the campaign
            if (Global.getCurrentState() == GameState.TITLE) {
                DiscordRp.showMenu();
            } else {
                String[] lines = PresenceText.buildCombat(engine);
                DiscordRp.show(lines[0], lines[1]);
            }
        } catch (Exception e) {
            log.warn("[DiscordRP] Error updating combat presence: " + e.getMessage());
        }
    }
}
