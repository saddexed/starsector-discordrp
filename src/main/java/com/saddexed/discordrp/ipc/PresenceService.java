package com.saddexed.discordrp.ipc;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class PresenceService {
    private static final Logger log = Logger.getLogger(PresenceService.class);

    static final long CONNECT_TIMEOUT = 10_000;
    static final long FIRST_RETRY = 5_000;
    static final long MAX_RETRY = 60_000;
    static final int BURST = 5;
    static final long BURST_WINDOW = 20_000;
    // Polled even when idle, to notice a dropped connection
    static final long IDLE = 15_000;

    private final Transport.Factory transports;
    private final String clientId;
    private final LongSupplier clock;

    private final Object lock = new Object();
    private Presence wanted;
    private boolean hasWanted;
    private boolean dirty;
    private volatile boolean running;

    // Worker thread only
    private IpcClient client;
    private Presence sent;
    private boolean hasSent;
    private long nextConnectAt;
    private long retryDelay = FIRST_RETRY;
    private boolean reportedOffline;
    private final long[] sendTimes = new long[BURST];
    private int sendIndex;

    public PresenceService(Transport.Factory transports, String clientId, LongSupplier clock) {
        this.transports = transports;
        this.clientId = clientId;
        this.clock = clock;
    }

    public void update(Presence presence) {
        synchronized (lock) {
            wanted = presence;
            hasWanted = true;
            dirty = true;
            lock.notifyAll();
        }
    }

    public void start() {
        if (running) return;
        running = true;
        Thread worker = new Thread(this::run, "DiscordRP-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void run() {
        while (running) {
            long wait;
            try {
                wait = step();
            } catch (RuntimeException e) {
                log.warn("[DiscordRP] Presence update failed", e);
                wait = FIRST_RETRY;
            }
            if (wait <= 0) continue;
            synchronized (lock) {
                if (!dirty) {
                    try {
                        lock.wait(wait);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

    long step() {
        long now = clock.getAsLong();
        Presence target;
        synchronized (lock) {
            if (!hasWanted) return IDLE;
            target = wanted;
            dirty = false;
        }

        if (client != null && !client.isOpen()) {
            log.info("[DiscordRP] Disconnected from Discord: " + client.closeReason());
            client = null;
            hasSent = false;
        }
        if (client == null) {
            if (now < nextConnectAt) return nextConnectAt - now;
            try {
                client = IpcClient.connect(transports.open(), clientId, CONNECT_TIMEOUT);
            } catch (IOException e) {
                if (!reportedOffline) {
                    log.info("[DiscordRP] Discord not available, will keep trying: " + e.getMessage());
                    reportedOffline = true;
                }
                nextConnectAt = now + retryDelay;
                retryDelay = Math.min(retryDelay * 2, MAX_RETRY);
                return nextConnectAt - now;
            }
            log.info("[DiscordRP] Connected to Discord.");
            retryDelay = FIRST_RETRY;
            reportedOffline = false;
        }

        if (hasSent && Objects.equals(target, sent)) return IDLE;

        long oldest = sendTimes[sendIndex];
        if (oldest != 0 && now - oldest < BURST_WINDOW) return BURST_WINDOW - (now - oldest);

        try {
            client.setActivity(target == null ? null : target.toJson());
        } catch (IOException e) {
            return 0;
        }
        sent = target;
        hasSent = true;
        sendTimes[sendIndex] = now;
        sendIndex = (sendIndex + 1) % BURST;
        return 0;
    }
}
