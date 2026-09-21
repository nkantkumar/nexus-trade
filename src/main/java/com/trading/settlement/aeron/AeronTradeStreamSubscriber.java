package com.trading.settlement.aeron;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.driver.MediaDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Low-latency Aeron Subscription listener to poll trade executions from Aeron channels
 * and deliver them directly into the Post-Trade Clearing Engine.
 */
public class AeronTradeStreamSubscriber implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AeronTradeStreamSubscriber.class);

    private final String channel;
    private final int streamId;
    private final Consumer<AeronTradeCodec.TradeEvent> tradeConsumer;
    private final String customDirectoryName;
    private Aeron aeron;
    private Subscription subscription;
    private MediaDriver mediaDriver;
    private final boolean ownsDriver;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AeronTradeStreamSubscriber(String channel, int streamId, Consumer<AeronTradeCodec.TradeEvent> tradeConsumer, boolean launchEmbeddedDriver) {
        this(channel, streamId, tradeConsumer, launchEmbeddedDriver, null);
    }

    public AeronTradeStreamSubscriber(String channel, int streamId, Consumer<AeronTradeCodec.TradeEvent> tradeConsumer, boolean launchEmbeddedDriver, String customDirectoryName) {
        this.channel = channel;
        this.streamId = streamId;
        this.tradeConsumer = tradeConsumer;
        this.ownsDriver = launchEmbeddedDriver;
        this.customDirectoryName = customDirectoryName;
    }

    public synchronized void start() {
        if (running.get()) return;

        if (ownsDriver) {
            log.info("Launching embedded Aeron MediaDriver for Subscriber...");
            mediaDriver = MediaDriver.launchEmbedded();
        }

        Aeron.Context ctx = new Aeron.Context();
        if (mediaDriver != null) {
            ctx.aeronDirectoryName(mediaDriver.aeronDirectoryName());
        } else if (customDirectoryName != null && !customDirectoryName.isBlank()) {
            ctx.aeronDirectoryName(customDirectoryName);
        }

        aeron = Aeron.connect(ctx);
        subscription = aeron.addSubscription(channel, streamId);
        running.set(true);
        log.info("Aeron Subscriber listening on channel: {} [streamId: {}]", channel, streamId);
    }

    public int poll(int fragmentLimit) {
        if (!running.get() || subscription == null) return 0;

        FragmentHandler fragmentHandler = (buffer, offset, length, header) -> {
            try {
                AeronTradeCodec.TradeEvent trade = AeronTradeCodec.decode(buffer, offset);
                log.debug("Subscriber received trade event: {}", trade.tradeId());
                tradeConsumer.accept(trade);
            } catch (Exception e) {
                log.error("Failed to decode Aeron trade message at offset {}", offset, e);
            }
        };

        return subscription.poll(fragmentHandler, fragmentLimit);
    }

    public boolean isConnected() {
        return subscription != null && subscription.isConnected();
    }

    @Override
    public synchronized void close() {
        if (!running.compareAndSet(true, false)) return;

        if (subscription != null) subscription.close();
        if (aeron != null) aeron.close();
        if (mediaDriver != null) mediaDriver.close();
        log.info("Aeron Subscriber closed successfully.");
    }
}
