package com.saddexed.discordrp.ui;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import com.saddexed.discordrp.presence.PresenceSettings;
import com.saddexed.discordrp.presence.PresenceText;
import org.lwjgl.input.Keyboard;

import java.util.Map;

public class SettingsDialog implements InteractionDialogPlugin {
    private static final Object DONE = new Object();
    private static final Object BACK = new Object();
    private static final Object INFO_ON = new Object();
    private static final Object INFO_TYPE = new Object();

    private enum Menu {
        INFO("Info"),
        STATUS("Status line");

        final String title;

        Menu(String title) {
            this.title = title;
        }
    }

    private InteractionDialogAPI dialog;
    private Menu menu;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("Choose what your Discord status shows");
        refresh();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == DONE) {
            dialog.dismiss();
            return;
        }
        if (optionData == BACK) {
            menu = null;
            refresh();
            return;
        }
        if (optionData == INFO_ON) {
            PresenceSettings.setShow(PresenceSettings.SHOW_INFO, !PresenceSettings.show(PresenceSettings.SHOW_INFO));
            PresenceText.push();
            refresh();
            return;
        }
        if (optionData == INFO_TYPE) {
            PresenceSettings.setShow(PresenceSettings.INFO_STATS, !PresenceSettings.show(PresenceSettings.INFO_STATS));
            PresenceText.push();
            refresh();
            return;
        }
        if (optionData instanceof Menu) {
            menu = (Menu) optionData;
            refresh();
            return;
        }
        if (optionData instanceof String) {
            String key = (String) optionData;
            PresenceSettings.setShow(key, !PresenceSettings.show(key));
            PresenceText.push();
            refresh();
        }
    }

    private void refresh() {
        TextPanelAPI text = dialog.getTextPanel();
        text.clear();

        text.addPara("Preview", Misc.getHighlightColor());
        String[] lines = PresenceText.build();
        text.addPara("Playing Starsector", Misc.getBasePlayerColor());
        addPreviewLine(text, lines == null ? null : lines[0]);
        addPreviewLine(text, lines == null ? null : lines[1]);
        text.addPara("Changes apply immediately and to every save.", Misc.getGrayColor());

        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        if (menu == null) {
            for (Menu each : Menu.values()) {
                options.addOption(each.title, each);
            }
            options.addOption("Done", DONE);
            options.setShortcut(DONE, Keyboard.KEY_ESCAPE, false, false, false, true);
        } else {
            if (menu == Menu.INFO) {
                boolean on = PresenceSettings.show(PresenceSettings.SHOW_INFO);
                options.addOption("Status: " + (on ? "On" : "Off"), INFO_ON);
                boolean stats = PresenceSettings.show(PresenceSettings.INFO_STATS);
                options.addOption("Type: " + (stats ? "Stats" : "Identity"), INFO_TYPE);
                options.setEnabled(INFO_TYPE, on);
                addOptions(options, stats ? PresenceSettings.STATS : PresenceSettings.IDENTITY, on);
            } else {
                addOptions(options, PresenceSettings.STATUS_LINE, true);
            }
            options.addOption("Back", BACK);
            options.setShortcut(BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
        }
    }

    private static void addPreviewLine(TextPanelAPI text, String line) {
        if (line == null) {
            text.addPara("(line hidden)", Misc.getGrayColor());
        } else {
            text.addPara(line);
        }
    }

    private static void addOptions(OptionPanelAPI options, String[][] items, boolean enabled) {
        for (String[] item : items) {
            String key = item[0];
            String needed = PresenceSettings.requires(key);
            boolean available = needed == null || PresenceSettings.isShown(needed);

            String box = PresenceSettings.isShown(key) ? "[x] " : "[  ] ";
            String label = box + item[1];
            if (!available) label += " (needs " + PresenceSettings.label(needed) + ")";

            options.addOption(label, key);
            if (!available) {
                // The stored tick is kept, so it comes back when the needed item does
                options.setEnabled(key, false);
                options.setTooltip(key, item[1] + " is only shown together with "
                        + PresenceSettings.label(needed) + ".");
            }
            if (!enabled) options.setEnabled(key, false);
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {}

    @Override
    public void advance(float amount) {}

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {}

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
