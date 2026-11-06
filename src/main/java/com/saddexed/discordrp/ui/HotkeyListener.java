package com.saddexed.discordrp.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.saddexed.discordrp.presence.PresenceSettings;

import java.util.List;

public class HotkeyListener implements CampaignInputListener {
    @Override
    public int getListenerInputPriority() {
        return 0;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
        int key = PresenceSettings.settingsKey();
        for (InputEventAPI event : events) {
            if (event.isConsumed() || !event.isKeyDownEvent()) continue;
            if (event.getEventValue() != key) continue;
            if (event.isCtrlDown() || event.isAltDown() || event.isShiftDown()) continue;

            CampaignUIAPI ui = Global.getSector().getCampaignUI();
            if (ui.isShowingDialog() || ui.isShowingMenu() || ui.getCurrentCoreTab() != null) return;

            event.consume();
            ui.showInteractionDialog(new SettingsDialog(), null);
            return;
        }
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {}

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {}
}
