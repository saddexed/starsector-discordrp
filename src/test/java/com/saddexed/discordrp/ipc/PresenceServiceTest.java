package com.saddexed.discordrp.ipc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.saddexed.discordrp.ipc.IpcClientTest.waitFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceServiceTest {
    private long now = 1_000_000;
    private boolean discordRunning = true;
    private final List<FakeDiscord> connections = new ArrayList<>();

    private final PresenceService service = new PresenceService(() -> {
        if (!discordRunning) throw new IOException("no pipe");
        FakeDiscord discord = new FakeDiscord();
        connections.add(discord);
        return discord;
    }, "123", () -> now);

    private static Presence presence(String details) {
        return new Presence(details, "Exploring", 1, null);
    }

    private FakeDiscord discord() {
        return connections.get(connections.size() - 1);
    }

    @Test
    void doesNothingUntilThereIsSomethingToShow() {
        assertEquals(PresenceService.IDLE, service.step());
        assertTrue(connections.isEmpty());
    }

    @Test
    void sendsOnceAndSkipsRepeats() {
        service.update(presence("Lv 8"));
        service.step();
        service.update(presence("Lv 8"));
        assertEquals(PresenceService.IDLE, service.step());

        assertEquals(1, connections.size());
        assertEquals(1, discord().activityCommands().size());
    }

    @Test
    void onlyTheNewestUpdateIsSent() throws Exception {
        service.update(presence("Lv 8"));
        service.update(presence("Lv 9"));
        service.step();
        assertEquals(1, discord().activityCommands().size());
        assertEquals("Lv 9", discord().activityCommands().get(0)
                .getJSONObject("args").getJSONObject("activity").getString("details"));
    }

    @Test
    void retriesWithBackoffWhileDiscordIsClosed() {
        discordRunning = false;
        service.update(presence("Lv 8"));
        assertEquals(PresenceService.FIRST_RETRY, service.step());

        now += 1000;
        assertEquals(PresenceService.FIRST_RETRY - 1000, service.step());

        now += PresenceService.FIRST_RETRY;
        assertEquals(PresenceService.FIRST_RETRY * 2, service.step());

        discordRunning = true;
        now += PresenceService.FIRST_RETRY * 2;
        service.step();
        assertEquals(1, discord().activityCommands().size());
    }

    @Test
    void reconnectsAndResendsAfterDiscordRestarts() throws Exception {
        service.update(presence("Lv 8"));
        service.step();
        FakeDiscord first = discord();
        first.hangUp();
        waitFor(first::isClosed);

        service.step();
        assertEquals(2, connections.size());
        assertEquals(1, discord().activityCommands().size());
    }

    @Test
    void keepsToTheRateLimit() {
        for (int i = 0; i < PresenceService.BURST; i++) {
            service.update(presence("Lv " + i));
            assertEquals(0, service.step());
        }
        service.update(presence("Lv 99"));
        assertEquals(PresenceService.BURST_WINDOW, service.step());

        now += PresenceService.BURST_WINDOW;
        assertEquals(0, service.step());
        assertEquals(PresenceService.BURST + 1, discord().activityCommands().size());
    }
}
