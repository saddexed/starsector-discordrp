package com.saddexed.discordrp.ipc;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrameTest {
    @Test
    void headerIsOpcodeThenLengthLittleEndian() throws Exception {
        ByteBuffer bytes = new Frame(Frame.Op.FRAME, new JSONObject().put("a", 1)).encode()
                .order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1, bytes.getInt(0));
        assertEquals("{\"a\":1}".length(), bytes.getInt(4));
        assertEquals(8 + 7, bytes.remaining());
    }

    @Test
    void roundTripsUnicode() throws Exception {
        JSONObject payload = new JSONObject().put("state", "Fighting against Ludd’s Fleet ¢");
        Frame frame = FakeDiscord.decode(new Frame(Frame.Op.PING, payload).encode());
        assertEquals(Frame.Op.PING, frame.op);
        assertEquals("Fighting against Ludd’s Fleet ¢", frame.payload.getString("state"));
    }

    @Test
    void rejectsUnknownOpcode() {
        ByteBuffer bytes = header(9, 0);
        assertThrows(IOException.class, () -> FakeDiscord.decode(bytes));
    }

    @Test
    void rejectsHugeLength() {
        ByteBuffer bytes = header(1, Frame.MAX_PAYLOAD + 1);
        assertThrows(IOException.class, () -> FakeDiscord.decode(bytes));
    }

    private static ByteBuffer header(int op, int length) {
        ByteBuffer bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(op).putInt(length).flip();
        return bytes;
    }
}
