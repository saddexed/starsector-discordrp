package com.saddexed.discordrp.ipc;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class Frame {
    public enum Op {
        HANDSHAKE, FRAME, CLOSE, PING, PONG;

        static Op of(int code) throws IOException {
            Op[] ops = values();
            if (code < 0 || code >= ops.length) throw new IOException("Unknown IPC opcode " + code);
            return ops[code];
        }
    }

    static final int MAX_PAYLOAD = 1 << 20;

    private static final int HEADER = 8;

    public final Op op;
    public final JSONObject payload;

    public Frame(Op op, JSONObject payload) {
        this.op = op;
        this.payload = payload;
    }

    public ByteBuffer encode() {
        byte[] json = payload.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(HEADER + json.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(op.ordinal()).putInt(json.length).put(json);
        buffer.flip();
        return buffer;
    }

    public static Frame read(Transport transport) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN);
        transport.readFully(header);
        header.flip();
        Op op = Op.of(header.getInt());
        int length = header.getInt();
        if (length < 0 || length > MAX_PAYLOAD) throw new IOException("Bad IPC payload length " + length);

        ByteBuffer body = ByteBuffer.allocate(length);
        transport.readFully(body);
        String json = new String(body.array(), StandardCharsets.UTF_8);
        try {
            return new Frame(op, json.isEmpty() ? new JSONObject() : new JSONObject(json));
        } catch (JSONException e) {
            throw new IOException("Bad IPC payload: " + e.getMessage(), e);
        }
    }
}
