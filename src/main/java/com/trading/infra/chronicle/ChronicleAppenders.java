package com.trading.infra.chronicle;

import java.util.concurrent.ConcurrentHashMap;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;

/**
 * Lazily opens Chronicle appenders so Spring contexts (including BDD) start without touching native
 * Chronicle initialization until the persistence path is actually used. Production JVMs should still
 * supply Chronicle-recommended {@code --add-opens} flags.
 */
public final class ChronicleAppenders {

    private static final ConcurrentHashMap<String, ExcerptAppender> APPENDERS = new ConcurrentHashMap<>();

    private ChronicleAppenders() {}

    public static ExcerptAppender forPath(String basePath) {
        return APPENDERS.computeIfAbsent(basePath, ChronicleAppenders::open);
    }

    private static ExcerptAppender open(String basePath) {
        return ChronicleQueue.singleBuilder(basePath).build().acquireAppender();
    }
}
