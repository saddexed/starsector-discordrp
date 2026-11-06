package com.saddexed.discordrp.presence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.FleetAIFlags;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.saddexed.discordrp.DiscordRp;

import java.util.Locale;

public final class PresenceText {
    private PresenceText() {}

    private static final float CHASE_RANGE = 2000f;

    public static String[] build() {
        if (Global.getSector() == null) return null;
        PersonAPI player = Global.getSector().getPlayerPerson();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (player == null || fleet == null) return null;

        LocationAPI location = fleet.getContainingLocation();
        String place = location == null ? null : location.getName();
        boolean article = location instanceof StarSystemAPI;

        StatusText.Kind kind;
        String subject = null;
        // The game opens a battle for any fleet interaction, friendly ones included,
        // so a fleet dialog wins over the battle check
        CampaignFleetAPI other = dialogFleet();
        CampaignFleetAPI enemy = other == null ? battleEnemy(fleet) : null;
        if (enemy != null && !enemy.isHostileTo(fleet)) enemy = null;
        MarketAPI market = dockedMarket();
        CampaignFleetAPI chaser = other == null && enemy == null ? chaser(fleet) : null;
        if (other != null) {
            kind = StatusText.Kind.INTERACTING;
            subject = fleetName(other) + " (" + relation(other, fleet) + ")";
        } else if (enemy != null) {
            kind = StatusText.Kind.FIGHTING;
            subject = fleetName(enemy);
        } else if (market != null) {
            CoreUITabId tab = Global.getSector().getCampaignUI().getCurrentCoreTab();
            if (tab == CoreUITabId.CARGO) {
                kind = StatusText.Kind.TRADING;
            } else if (tab == CoreUITabId.REFIT) {
                kind = StatusText.Kind.REFITTING;
            } else {
                kind = StatusText.Kind.DOCKED;
            }
            subject = StatusText.marketWithSystem(market.getName(), article ? place : null);
        } else if (chaser != null) {
            kind = chaser.isHostileTo(fleet) ? StatusText.Kind.FLEEING : StatusText.Kind.PURSUED;
            subject = fleetName(chaser);
        } else if (location != null && location.isHyperspace()) {
            kind = StatusText.Kind.TRAVELLING;
        } else {
            kind = StatusText.Kind.EXPLORING;
        }

        String status = StatusText.describe(kind, subject, place, article,
                PresenceSettings.isShown(PresenceSettings.SHOW_ACTIVITY),
                PresenceSettings.isShown(PresenceSettings.SHOW_LOCATION));

        String info = PresenceSettings.show(PresenceSettings.SHOW_INFO) ? infoLine(player, fleet) : null;
        return new String[] {info, status};
    }

    private static String infoLine(PersonAPI player, CampaignFleetAPI fleet) {
        String title = null;
        if (PresenceSettings.isShown(PresenceSettings.SHOW_TITLE)) {
            // Empty on some saves; the game then falls back to Sir/Ma'am
            title = Global.getSector().getCharacterData().getHonorific();
            if (title == null || title.trim().isEmpty()) {
                title = player.isMale() ? Misc.SIR : Misc.MAAM;
            }
        }
        String name = PresenceSettings.isShown(PresenceSettings.SHOW_NAME)
                ? player.getName().getFullName() : null;
        String level = PresenceSettings.isShown(PresenceSettings.SHOW_LEVEL)
                ? "Lv " + player.getStats().getLevel() : null;
        String credits = PresenceSettings.isShown(PresenceSettings.SHOW_CREDITS)
                ? formatCredits((long) fleet.getCargo().getCredits().get()) + "¢" : null;

        String averageCR = null;
        if (PresenceSettings.isShown(PresenceSettings.SHOW_AVERAGE_CR)) {
            Float cr = averageCR(fleet);
            if (cr != null) averageCR = PresenceLine.percent(cr) + " CR";
        }

        String flagshipInfo = null;
        FleetMemberAPI flagship = fleet.getFlagship();
        if (flagship != null) {
            flagshipInfo = PresenceLine.flagship(
                    PresenceSettings.isShown(PresenceSettings.SHOW_SHIP_NAME)
                            ? flagship.getShipName() : null,
                    PresenceSettings.isShown(PresenceSettings.SHOW_SHIP_HULL)
                            ? flagship.getHullSpec().getHullName() : null);
        }

        String faction = PresenceSettings.isShown(PresenceSettings.SHOW_FACTION)
                ? factionName(Global.getSector().getPlayerFaction().getDisplayName()) : null;

        if (PresenceSettings.show(PresenceSettings.INFO_STATS)) {
            return PresenceLine.join(PresenceLine.SEPARATOR, averageCR, credits, level, faction);
        }
        return PresenceLine.join(PresenceLine.SEPARATOR, PresenceLine.join(" ", title, name), flagshipInfo);
    }

    public static String[] buildCombat(CombatEngineAPI engine) {
        StatusText.Kind kind;
        String subject = null;
        if (engine.isSimulation()) {
            kind = StatusText.Kind.SIMULATOR;
        } else if (engine.isMission()) {
            kind = StatusText.Kind.MISSION;
        } else {
            BattleCreationContext context = engine.getContext();
            CampaignFleetAPI enemy = null;
            if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
                enemy = battleEnemy(Global.getSector().getPlayerFleet());
            }
            if (enemy == null && context != null) enemy = context.getOtherFleet();
            subject = enemy == null ? null : fleetName(enemy);

            if (context != null && context.getPlayerGoal() == FleetGoal.ESCAPE) {
                kind = StatusText.Kind.FLEEING;
            } else if (context != null && context.getOtherGoal() == FleetGoal.ESCAPE) {
                kind = StatusText.Kind.PURSUING;
            } else {
                kind = StatusText.Kind.FIGHTING;
            }
        }

        String status = StatusText.describe(kind, subject, null, false,
                PresenceSettings.isShown(PresenceSettings.SHOW_ACTIVITY),
                PresenceSettings.isShown(PresenceSettings.SHOW_LOCATION));

        if (status != null && kind == StatusText.Kind.FIGHTING) {
            status = status + " (" + PresenceLine.percent(cleanDisengageProgress(engine)) + ")";
        }

        ShipAPI ship = engine.getPlayerShip();
        String info = ship == null || !ship.isAlive() || !PresenceSettings.show(PresenceSettings.SHOW_INFO) ? null
                : PresenceLine.shipStats(ship.getHullLevel(), armourLevel(ship), ship.getCurrentCR());
        return new String[] {info, status};
    }

    // The enemy side's manager tracks the player's progress as "its enemy" disengaging
    private static float cleanDisengageProgress(CombatEngineAPI engine) {
        CombatFleetManagerAPI enemySide = engine.getFleetManager(FleetSide.ENEMY);
        if (enemySide == null) return 0f;
        float threshold = enemySide.getEnemyCleanDisengageThreshold();
        if (threshold <= 0f) return 1f;
        return enemySide.getEnemyCleanDisengagePoints() / threshold;
    }

    private static float armourLevel(ShipAPI ship) {
        ArmorGridAPI armour = ship.getArmorGrid();
        if (armour == null || armour.getMaxArmorInCell() <= 0f) return 0f;
        float[][] grid = armour.getGrid();
        float total = 0f;
        int cells = 0;
        for (float[] column : grid) {
            for (float cell : column) {
                total += cell;
                cells++;
            }
        }
        return cells == 0 ? 0f : total / (cells * armour.getMaxArmorInCell());
    }

    private static Float averageCR(CampaignFleetAPI fleet) {
        float total = 0f;
        int count = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            total += member.getRepairTracker().getCR();
            count++;
        }
        return count == 0 ? null : total / count;
    }

    private static String fleetName(CampaignFleetAPI fleet) {
        String name = fleet.getNameWithFactionKeepCase();
        if (name == null || name.trim().isEmpty()) name = fleet.getName();
        if (name == null || name.trim().isEmpty()) name = fleet.getFaction().getDisplayName();
        return PresenceLine.capitalize(name);
    }

    private static CampaignFleetAPI battleEnemy(CampaignFleetAPI fleet) {
        BattleAPI battle = fleet.getBattle();
        if (battle == null) return null;
        return battle.getPrimary(battle.getNonPlayerSide());
    }

    private static CampaignFleetAPI dialogFleet() {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null || !(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) return null;
        return (CampaignFleetAPI) dialog.getInteractionTarget();
    }

    private static String relation(CampaignFleetAPI other, CampaignFleetAPI fleet) {
        if (other.isHostileTo(fleet)) return "hostile";
        RepLevel level = other.getFaction().getRelationshipLevel(Factions.PLAYER);
        return level.isAtWorst(RepLevel.FAVORABLE) ? "friendly" : "neutral";
    }

    private static MarketAPI dockedMarket() {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null || dialog.getInteractionTarget() == null) return null;
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        return market == null || market.isPlanetConditionMarketOnly() ? null : market;
    }

    private static CampaignFleetAPI chaser(CampaignFleetAPI fleet) {
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return null;
        // Nearest hostile chaser wins; friendly ones (patrols wanting a scan) only otherwise
        CampaignFleetAPI nearest = null;
        float nearestDist = CHASE_RANGE;
        CampaignFleetAPI friendly = null;
        float friendlyDist = CHASE_RANGE;
        for (CampaignFleetAPI other : location.getFleets()) {
            if (other == fleet || !isChasing(other, fleet)) continue;
            if (other.getVisibilityLevelToPlayerFleet() == VisibilityLevel.NONE) continue;
            if (other.getAI() != null && other.getAI().isFleeing()) continue;
            float dist = Misc.getDistance(fleet, other);
            if (!other.isHostileTo(fleet)) {
                if (dist < friendlyDist) {
                    friendly = other;
                    friendlyDist = dist;
                }
            } else if (dist < nearestDist) {
                nearest = other;
                nearestDist = dist;
            }
        }
        return nearest != null ? nearest : friendly;
    }

    // The interaction target is only set right before contact; during the chase the AI
    // keeps the player as its tactical target and in its pursuit memory flag
    private static boolean isChasing(CampaignFleetAPI other, CampaignFleetAPI fleet) {
        if (other.getInteractionTarget() == fleet) return true;
        if (other.getAI() instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical = ((ModularFleetAIAPI) other.getAI()).getTacticalModule();
            if (tactical != null && tactical.getTarget() == fleet) return true;
        }
        return other.getMemoryWithoutUpdate().get(FleetAIFlags.PURSUIT_TARGET) == fleet;
    }

    public static void push() {
        String[] lines = build();
        if (lines != null) {
            DiscordRp.show(lines[0], lines[1]);
        }
    }

    // The player faction is called "Your" until the player names it
    static String factionName(String displayName) {
        return displayName == null || displayName.trim().equalsIgnoreCase("Your") ? null : displayName;
    }

    static String formatCredits(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }
}
