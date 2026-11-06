package com.saddexed.discordrp;

import com.fs.starfarer.api.EveryFrameScript;
import com.saddexed.discordrp.presence.PresenceText;
import org.apache.log4j.Logger;

public class CampaignPresenceScript implements EveryFrameScript {
    private static final Logger log = Logger.getLogger(CampaignPresenceScript.class);

    private static final long INTERVAL_MILLIS = 5_000;

    private long lastPush;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        long now = System.currentTimeMillis();
        if (now - lastPush < INTERVAL_MILLIS) return;
        lastPush = now;
        try {
            PresenceText.push();
        } catch (RuntimeException e) {
            log.warn("[DiscordRP] Could not build the campaign presence", e);
        }
    }
}
