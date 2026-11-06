package com.saddexed.discordrp.ipc;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

final class FakeDiscord implements Transport {
    private static final ByteBuffer HANG_UP = ByteBuffer.allocate(0);

    final List<Frame> written = new CopyOnWriteArrayList<>();
    private final BlockingQueue<ByteBuffer> incoming = new LinkedBlockingQueue<>();
    private ByteBuffer current;
    private volatile boolean closed;

    volatile Frame handshakeReply = ready();

    static Frame ready() {
        try {
            return new Frame(Frame.Op.FRAME, new JSONObject("{\"cmd\":\"DISPATCH\",\"evt\":\"READY\",\"data\":{\"v\":1}}"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void send(Frame frame) {
        incoming.add(frame.encode());
    }

    void hangUp() {
        incoming.add(HANG_UP);
    }

    boolean isClosed() {
        return closed;
    }

    List<JSONObject> activityCommands() {
        List<JSONObject> commands = new CopyOnWriteArrayList<>();
        for (Frame frame : written) {
            if (frame.op == Frame.Op.FRAME && "SET_ACTIVITY".equals(frame.payload.optString("cmd"))) {
                commands.add(frame.payload);
            }
        }
        return commands;
    }

    @Override
    public void write(ByteBuffer data) throws IOException {
        if (closed) throw new IOException("closed");
        ByteBuffer copy = ByteBuffer.allocate(data.remaining());
        copy.put(data).flip();
        Frame frame = decode(copy);
        written.add(frame);
        if (frame.op == Frame.Op.HANDSHAKE && handshakeReply != null) send(handshakeReply);
    }

    @Override
    public void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (current == null || !current.hasRemaining()) {
                try {
                    current = incoming.take();
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
                if (current == HANG_UP) {
                    incoming.add(HANG_UP);
                    throw new IOException("pipe closed");
                }
            }
            while (buffer.hasRemaining() && current.hasRemaining()) buffer.put(current.get());
        }
    }

    @Override
    public void close() {
        closed = true;
        hangUp();
    }

    static Frame decode(ByteBuffer bytes) throws IOException {
        return Frame.read(new Transport() {
            @Override
            public void write(ByteBuffer data) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void readFully(ByteBuffer buffer) throws IOException {
                if (buffer.remaining() > bytes.remaining()) throw new IOException("short frame");
                while (buffer.hasRemaining()) buffer.put(bytes.get());
            }

            @Override
            public void close() {
            }
        });
    }
}
