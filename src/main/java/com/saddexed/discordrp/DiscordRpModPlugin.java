package com.saddexed.discordrp;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.saddexed.discordrp.ui.HotkeyListener;

public class DiscordRpModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        DiscordRp.start();
        DiscordRp.showMenu();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        DiscordRp.restartTimer();
        Global.getSector().addTransientScript(new CampaignPresenceScript());
        Global.getSector().getListenerManager().addListener(new HotkeyListener(), true);
    }
}
