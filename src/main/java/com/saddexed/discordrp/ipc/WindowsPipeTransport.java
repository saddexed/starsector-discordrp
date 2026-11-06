package com.saddexed.discordrp.ipc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// AsynchronousFileChannel because Starsector's sandbox blocks RandomAccessFile, and a plain FileChannel
// shares one lock between reads and writes, so writes would hang behind the reader thread's blocking read.
public final class WindowsPipeTransport implements Transport {
    private static final int PIPES = 10;

    private static final ExecutorService IO = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "DiscordRP-IO");
        thread.setDaemon(true);
        return thread;
    });

    private final AsynchronousFileChannel channel;

    private WindowsPipeTransport(AsynchronousFileChannel channel) {
        this.channel = channel;
    }

    public static final Factory FACTORY = WindowsPipeTransport::open;

    public static WindowsPipeTransport open() throws IOException {
        IOException last = null;
        for (int i = 0; i < PIPES; i++) {
            try {
                return new WindowsPipeTransport(AsynchronousFileChannel.open(
                        Paths.get("\\\\.\\pipe\\discord-ipc-" + i),
                        EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE), IO));
            } catch (IOException e) {
                last = e;
            }
        }
        throw new IOException("No Discord pipe found (is Discord running?)", last);
    }

    @Override
    public void write(ByteBuffer data) throws IOException {
        // Pipes have no position; 0 is ignored
        while (data.hasRemaining()) await(channel.write(data, 0));
    }

    @Override
    public void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (await(channel.read(buffer, 0)) < 0) throw new IOException("Discord closed the pipe");
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private static int await(Future<Integer> pending) throws IOException {
        try {
            return pending.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
        }
    }
}
