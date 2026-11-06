package com.saddexed.discordrp.ipc;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpcClientTest {
    @Test
    void handshakeSendsVersionAndClientId() throws Exception {
        FakeDiscord discord = new FakeDiscord();
        IpcClient client = IpcClient.connect(discord, "123", 2000);

        Frame handshake = discord.written.get(0);
        assertEquals(Frame.Op.HANDSHAKE, handshake.op);
        assertEquals(1, handshake.payload.getInt("v"));
        assertEquals("123", handshake.payload.getString("client_id"));
        assertTrue(client.isOpen());
        client.close();
    }

    @Test
    void setActivitySendsCommand() throws Exception {
        FakeDiscord discord = new FakeDiscord();
        IpcClient client = IpcClient.connect(discord, "123", 2000);
        client.setActivity(new JSONObject().put("details", "Lv 8"));
        client.setActivity(null);

        JSONObject set = discord.activityCommands().get(0);
        assertEquals("Lv 8", set.getJSONObject("args").getJSONObject("activity").getString("details"));
        assertTrue(set.getJSONObject("args").has("pid"));
        assertFalse(set.getString("nonce").isEmpty());
        assertTrue(discord.activityCommands().get(1).getJSONObject("args").isNull("activity"));
        client.close();
    }

    @Test
    void refusedHandshakeThrowsWithDiscordsMessage() throws Exception {
        FakeDiscord discord = new FakeDiscord();
        discord.handshakeReply = new Frame(Frame.Op.CLOSE,
                new JSONObject().put("code", 4000).put("message", "Invalid Client ID"));
        IOException e = assertThrows(IOException.class, () -> IpcClient.connect(discord, "1", 2000));
        assertTrue(e.getMessage().contains("Invalid Client ID"), e.getMessage());
        assertTrue(discord.isClosed());
    }

    @Test
    void silentDiscordTimesOut() {
        FakeDiscord discord = new FakeDiscord();
        discord.handshakeReply = null;
        assertThrows(IOException.class, () -> IpcClient.connect(discord, "1", 100));
        assertTrue(discord.isClosed());
    }

    @Test
    void answersPings() throws Exception {
        FakeDiscord discord = new FakeDiscord();
        IpcClient client = IpcClient.connect(discord, "1", 2000);
        discord.send(new Frame(Frame.Op.PING, new JSONObject().put("n", 7)));

        waitFor(() -> discord.written.stream().anyMatch(f -> f.op == Frame.Op.PONG));
        Frame pong = discord.written.stream().filter(f -> f.op == Frame.Op.PONG).findFirst().get();
        assertEquals(7, pong.payload.getInt("n"));
        client.close();
    }

    @Test
    void noticesWhenDiscordQuits() throws Exception {
        FakeDiscord discord = new FakeDiscord();
        IpcClient client = IpcClient.connect(discord, "1", 2000);
        discord.hangUp();
        waitFor(() -> !client.isOpen());
        assertThrows(IOException.class, () -> client.setActivity(null));
    }

    static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("timed out");
            Thread.sleep(5);
        }
    }
}
