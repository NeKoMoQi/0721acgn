package com.example.demo.service.agent;

import java.util.concurrent.CancellationException;

/** One request's deadline and upstream connection, also closed on browser disconnect. */
public final class AgentRun {
    private final long deadline = System.nanoTime() + 90_000_000_000L;
    private volatile boolean cancelled;
    private Runnable cancelRequest;

    public void check() {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new CancellationException();
        if (System.nanoTime() > deadline) throw new IllegalStateException("规划超时，请缩小城市范围或稍后重试");
    }

    public synchronized void attach(Runnable cancelRequest) {
        check();
        this.cancelRequest = cancelRequest;
    }

    public synchronized void detach() { cancelRequest = null; }

    public void cancel() {
        Runnable action;
        synchronized (this) { cancelled = true; action = cancelRequest; }
        if (action != null) action.run();
    }
}
