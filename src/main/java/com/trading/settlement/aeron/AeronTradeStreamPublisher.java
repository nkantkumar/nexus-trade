package com.trading.settlement.aeron;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-throughput Aeron Publisher for streaming execution reports and equity trades
 * over IPC / UDP channels into the clearing & settlement engine.
 */
public class AeronTradeStreamPublisher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AeronTradeStreamPublisher.class);

    private final String channel;
    private final int streamId;
    private final String customDirectoryName;
    private Aeron aeron;
    private Publication publication;
    private MediaDriver mediaDriver;
    private final boolean ownsDriver;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AeronTradeStreamPublisher(String channel, int streamId, boolean launchEmbeddedDriver) {
        this(channel, streamId, launchEmbeddedDriver, null);
    }

    public AeronTradeStreamPublisher(String channel, int streamId, boolean launchEmbeddedDriver, String customDirectoryName) {
        this.channel = channel;
        this.streamId = streamId;
        this.ownsDriver = launchEmbeddedDriver;
        this.customDirectoryName = customDirectoryName;
    }

    public synchronized void start() {
        if (running.get()) return;

        if (ownsDriver) {
            log.info("Launching embedded Aeron MediaDriver for Publisher...");
            mediaDriver = MediaDriver.launchEmbedded();
        }

        Aeron.Context ctx = new Aeron.Context();
        if (mediaDriver != null) {
            ctx.aeronDirectoryName(mediaDriver.aeronDirectoryName());
        } else if (customDirectoryName != null && !customDirectoryName.isBlank()) {
            ctx.aeronDirectoryName(customDirectoryName);
        }

        aeron = Aeron.connect(ctx);
        publication = aeron.addPublication(channel, streamId);
        running.set(true);
        log.info("Aeron Publisher started on channel: {} [streamId: {}]", channel, streamId);
    }

    public boolean publishTrade(AeronTradeCodec.TradeEvent trade) {
        if (!running.get() || publication == null) {
            throw new IllegalStateException("Aeron Publisher is not started.");
        }

        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(AeronTradeCodec.MESSAGE_LENGTH));
        AeronTradeCodec.encode(trade, buffer, 0);

        long result = publication.offer(buffer, 0, AeronTradeCodec.MESSAGE_LENGTH);
        if (result > 0) {
            log.debug("Published trade {} to Aeron stream successfully.", trade.tradeId());
            return true;
        } else if (result == Publication.BACK_PRESSURED) {
            log.warn("Aeron offer back-pressured for trade {}", trade.tradeId());
        } else if (result == Publication.NOT_CONNECTED) {
            log.warn("Aeron publication not connected to any subscribers yet.");
        } else {
            log.error("Aeron offer failed with result code: {}", result);
        }
        return false;
    }

    public boolean isConnected() {
        return publication != null && publication.isConnected();
    }

    public String getAeronDirectoryName() {
        if (mediaDriver != null) return mediaDriver.aeronDirectoryName();
        return customDirectoryName;
    }

    @Override
    public synchronized void close() {
        if (!running.compareAndSet(true, false)) return;

        if (publication != null) publication.close();
        if (aeron != null) aeron.close();
        if (mediaDriver != null) mediaDriver.close();
        log.info("Aeron Publisher closed successfully.");
    }
}
