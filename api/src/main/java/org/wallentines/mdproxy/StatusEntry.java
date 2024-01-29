package org.wallentines.mdproxy;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class StatusEntry implements Comparable<StatusEntry> {
    protected final int priority;
    protected final WrappedRequirement requirement;

    protected StatusEntry(int priority, WrappedRequirement requirement) {
        this.priority = priority;
        this.requirement = requirement;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(@NotNull StatusEntry o) {
        return o.priority - priority;
    }

    public abstract CompletableFuture<String> generateReply(ClientConnection conn, Proxy proxy);

}
