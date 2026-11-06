package com.saddexed.discordrp.ipc;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnixSocketTransportTest {
    @Test
    void candidatesUseTheFirstRuntimeDirThatIsSet() {
        Map<String, String> env = new HashMap<>();
        env.put("TMPDIR", "/var/tmp");
        env.put("XDG_RUNTIME_DIR", "/run/user/1000");
        List<Path> paths = UnixSocketTransport.candidates(env);

        assertEquals(40, paths.size());
        assertEquals(Paths.get("/run/user/1000", "discord-ipc-0"), paths.get(0));
        assertEquals(Paths.get("/run/user/1000/app/com.discordapp.Discord", "discord-ipc-0"), paths.get(1));
        assertEquals(Paths.get("/run/user/1000/snap.discord", "discord-ipc-0"), paths.get(2));
        assertEquals(Paths.get("/run/user/1000", "discord-ipc-1"), paths.get(4));
    }

    @Test
    void candidatesFallBackToTmp() {
        assertEquals(Paths.get("/tmp", "discord-ipc-0"),
                UnixSocketTransport.candidates(Collections.emptyMap()).get(0));
        assertEquals(Paths.get("/private/tmp/x", "discord-ipc-0"),
                UnixSocketTransport.candidates(Collections.singletonMap("TMPDIR", "/private/tmp/x")).get(0));
    }

    @Test
    void noSocketMeansDiscordIsNotRunning(@TempDir Path dir) {
        assertThrows(IOException.class,
                () -> UnixSocketTransport.open(Collections.singletonList(dir.resolve("discord-ipc-0"))));
    }

    @Test
    void handshakeAndActivityOverARealSocket(@TempDir Path dir) throws Exception {
        Path socket = dir.resolve("discord-ipc-1");
        try (ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            server.bind(UnixDomainSocketAddress.of(socket));

            CompletableFuture<Frame> command = CompletableFuture.supplyAsync(() -> {
                try (UnixSocketTransport discord = new UnixSocketTransport(server.accept())) {
                    Frame handshake = Frame.read(discord);
                    assertEquals(Frame.Op.HANDSHAKE, handshake.op);
                    discord.write(FakeDiscord.ready().encode());
                    return Frame.read(discord);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Transport transport = UnixSocketTransport.open(Arrays.asList(dir.resolve("discord-ipc-0"), socket));
            IpcClient client = IpcClient.connect(transport, "123", 2000);
            client.setActivity(new JSONObject().put("details", "Lv 8"));

            Frame set = command.get(2, TimeUnit.SECONDS);
            assertEquals("SET_ACTIVITY", set.payload.getString("cmd"));
            assertEquals("Lv 8", set.payload.getJSONObject("args").getJSONObject("activity").getString("details"));
            client.close();
        }
    }

    @Test
    void picksTransportByOs() {
        assertEquals(WindowsPipeTransport.FACTORY, Transport.forOs("Windows 11"));
        assertEquals(UnixSocketTransport.FACTORY, Transport.forOs("Linux"));
        assertEquals(UnixSocketTransport.FACTORY, Transport.forOs("Mac OS X"));
    }
}
