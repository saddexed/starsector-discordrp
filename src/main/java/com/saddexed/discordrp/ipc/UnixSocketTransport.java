package com.saddexed.discordrp.ipc;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UnixSocketTransport implements Transport {
    private static final int SOCKETS = 10;

    private static final String[] SUBDIRS = {
            "",                                       // regular install, macOS
            "app/com.discordapp.Discord",             // Flatpak
            "snap.discord",                           // Snap
            ".flatpak/dev.vencord.Vesktop/xdg-run",   // Vesktop Flatpak
    };

    private final SocketChannel channel;

    UnixSocketTransport(SocketChannel channel) {
        this.channel = channel;
    }

    public static final Factory FACTORY = () -> open(candidates(System.getenv()));

    static List<Path> candidates(Map<String, String> env) {
        String base = "/tmp";
        for (String name : new String[] {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
            String value = env.get(name);
            if (value != null && !value.trim().isEmpty()) {
                base = value.trim();
                break;
            }
        }
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < SOCKETS; i++) {
            for (String subdir : SUBDIRS) {
                paths.add(Paths.get(base, subdir, "discord-ipc-" + i));
            }
        }
        return paths;
    }

    static UnixSocketTransport open(List<Path> paths) throws IOException {
        IOException last = null;
        for (Path path : paths) {
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            try {
                channel.connect(UnixDomainSocketAddress.of(path));
                return new UnixSocketTransport(channel);
            } catch (IOException e) {
                channel.close();
                last = e;
            }
        }
        throw new IOException("No Discord socket found (is Discord running?)", last);
    }

    @Override
    public void write(ByteBuffer data) throws IOException {
        while (data.hasRemaining()) channel.write(data);
    }

    @Override
    public void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) throw new IOException("Discord closed the socket");
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
