package com.saddexed.discordrp.ipc;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class IpcClient implements Closeable {
    private static final Logger log = Logger.getLogger(IpcClient.class);

    private final Transport transport;
    private final Object writeLock = new Object();
    private final CountDownLatch ready = new CountDownLatch(1);
    private volatile boolean open = true;
    private volatile String closeReason;

    private IpcClient(Transport transport) {
        this.transport = transport;
    }

    public static IpcClient connect(Transport transport, String clientId, long timeoutMillis) throws IOException {
        IpcClient client = new IpcClient(transport);
        try {
            client.send(Frame.Op.HANDSHAKE, new JSONObject().put("v", 1).put("client_id", clientId));
        } catch (IOException | JSONException e) {
            client.close();
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }

        Thread reader = new Thread(client::readLoop, "DiscordRP-Reader");
        reader.setDaemon(true);
        reader.start();

        try {
            if (!client.ready.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                client.close();
                throw new IOException("Discord did not answer the handshake");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            client.close();
            throw new IOException("Interrupted", e);
        }
        if (!client.open) throw new IOException("Discord refused the connection: " + client.closeReason);
        return client;
    }

    public void setActivity(JSONObject activity) throws IOException {
        try {
            JSONObject args = new JSONObject()
                    .put("pid", ProcessId.get())
                    .put("activity", activity == null ? JSONObject.NULL : activity);
            send(Frame.Op.FRAME, new JSONObject()
                    .put("cmd", "SET_ACTIVITY")
                    .put("args", args)
                    .put("nonce", UUID.randomUUID().toString()));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public String closeReason() {
        return closeReason;
    }

    @Override
    public void close() {
        open = false;
        try {
            transport.close();
        } catch (IOException ignored) {
        }
        ready.countDown();
    }

    private void send(Frame.Op op, JSONObject payload) throws IOException {
        if (!open) throw new IOException("Connection closed");
        synchronized (writeLock) {
            try {
                transport.write(new Frame(op, payload).encode());
            } catch (IOException e) {
                end("write failed: " + e.getMessage());
                throw e;
            }
        }
    }

    private void readLoop() {
        try {
            while (open) {
                handle(Frame.read(transport));
            }
        } catch (IOException e) {
            if (open) end(e.getMessage());
        } catch (RuntimeException e) {
            log.warn("[DiscordRP] IPC reader failed", e);
            end(e.toString());
        }
    }

    private void handle(Frame frame) throws IOException {
        switch (frame.op) {
            case PING:
                send(Frame.Op.PONG, frame.payload);
                break;
            case CLOSE:
                end(frame.payload.optString("message", "closed by Discord"));
                break;
            case FRAME:
                String cmd = frame.payload.optString("cmd");
                String evt = frame.payload.optString("evt");
                if ("DISPATCH".equals(cmd) && "READY".equals(evt)) {
                    ready.countDown();
                } else if ("ERROR".equals(evt)) {
                    JSONObject data = frame.payload.optJSONObject("data");
                    log.warn("[DiscordRP] Discord rejected " + cmd + ": "
                            + (data == null ? frame.payload : data.optString("message", data.toString())));
                }
                break;
            default:
                break;
        }
    }

    private void end(String reason) {
        if (closeReason == null) closeReason = reason;
        close();
    }

    // Discord clears the activity by itself when this process exits
    static final class ProcessId {
        private static Long pid;

        static synchronized long get() {
            if (pid == null) {
                long value = 0;
                try {
                    String name = ManagementFactory.getRuntimeMXBean().getName();
                    value = Long.parseLong(name.substring(0, name.indexOf('@')));
                } catch (RuntimeException | LinkageError ignored) {
                }
                pid = value;
            }
            return pid;
        }
    }
}
