package com.trading.settlement.aeron;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AeronTradeStreamTest {

    @Test
    public void testAeronTradeCodecEncodeDecode() {
        AeronTradeCodec.TradeEvent original = new AeronTradeCodec.TradeEvent(
                "TRD-998811",
                "ORD-BUY-100",
                "ORD-SELL-200",
                "ACC-BUY-01",
                "ACC-SELL-02",
                "AAPL",
                500,
                185.50,
                1700000000000L,
                "USD"
        );

        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(AeronTradeCodec.MESSAGE_LENGTH));
        int encodedBytes = AeronTradeCodec.encode(original, buffer, 0);

        assertEquals(AeronTradeCodec.MESSAGE_LENGTH, encodedBytes);

        AeronTradeCodec.TradeEvent decoded = AeronTradeCodec.decode(buffer, 0);

        assertEquals(original.tradeId(), decoded.tradeId());
        assertEquals(original.buyOrderId(), decoded.buyOrderId());
        assertEquals(original.sellOrderId(), decoded.sellOrderId());
        assertEquals(original.buyAccountId(), decoded.buyAccountId());
        assertEquals(original.sellAccountId(), decoded.sellAccountId());
        assertEquals(original.symbol(), decoded.symbol());
        assertEquals(original.quantity(), decoded.quantity());
        assertEquals(original.price(), decoded.price(), 0.0001);
        assertEquals(original.timestamp(), decoded.timestamp());
        assertEquals(original.currency(), decoded.currency());
    }

    @Test
    public void testAeronPubSubLoopback() throws Exception {
        String channel = "aeron:ipc";
        int streamId = 10001;

        AtomicReference<AeronTradeCodec.TradeEvent> receivedTrade = new AtomicReference<>();

        try (AeronTradeStreamPublisher publisher = new AeronTradeStreamPublisher(channel, streamId, true)) {
            publisher.start();

            String aeronDir = publisher.getAeronDirectoryName();
            try (AeronTradeStreamSubscriber subscriber = new AeronTradeStreamSubscriber(channel, streamId, receivedTrade::set, false, aeronDir)) {

                subscriber.start();

                // Give Aeron driver time to establish connection
                int retries = 0;
                while (!publisher.isConnected() && retries < 50) {
                    Thread.sleep(50);
                    retries++;
                }

                AeronTradeCodec.TradeEvent trade = new AeronTradeCodec.TradeEvent(
                        "TRD-AERON-1", "B-1", "S-1", "ACC-1", "ACC-2", "MSFT", 100, 420.00, System.currentTimeMillis(), "USD"
                );

                boolean published = false;
                for (int i = 0; i < 20; i++) {
                    if (publisher.publishTrade(trade)) {
                        published = true;
                        break;
                    }
                    Thread.sleep(50);
                }

                assertTrue(published, "Trade should be offered to Aeron stream.");

                // Poll subscriber
                for (int i = 0; i < 20; i++) {
                    subscriber.poll(10);
                    if (receivedTrade.get() != null) break;
                    Thread.sleep(50);
                }

                assertNotNull(receivedTrade.get(), "Subscriber should receive trade event via Aeron IPC.");
                assertEquals("TRD-AERON-1", receivedTrade.get().tradeId());
                assertEquals("MSFT", receivedTrade.get().symbol());
            }
        }
    }
}
