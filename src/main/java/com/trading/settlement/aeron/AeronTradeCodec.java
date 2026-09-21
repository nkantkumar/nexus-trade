package com.trading.settlement.aeron;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * High-performance zero-copy binary encoder/decoder for Equity Trade Execution events
 * transmitted via Aeron IPC/UDP messaging channel.
 */
public class AeronTradeCodec {

    public record TradeEvent(
            String tradeId,
            String buyOrderId,
            String sellOrderId,
            String buyAccountId,
            String sellAccountId,
            String symbol,
            long quantity,
            double price,
            long timestamp,
            String currency
    ) {}

    // Message layout offsets (binary format)
    // [0..35] tradeId (36 chars)
    // [36..71] buyOrderId (36 chars)
    // [72..107] sellOrderId (36 chars)
    // [108..123] buyAccountId (16 chars fixed)
    // [124..139] sellAccountId (16 chars fixed)
    // [140..147] symbol (8 chars fixed)
    // [148..155] quantity (int64)
    // [156..163] price (float64)
    // [164..171] timestamp (int64)
    // [172..174] currency (3 chars fixed)
    public static final int MESSAGE_LENGTH = 175;

    public static int encode(TradeEvent trade, UnsafeBuffer buffer, int offset) {
        buffer.putBytes(offset, padRight(trade.tradeId(), 36).getBytes(StandardCharsets.UTF_8));
        buffer.putBytes(offset + 36, padRight(trade.buyOrderId(), 36).getBytes(StandardCharsets.UTF_8));
        buffer.putBytes(offset + 72, padRight(trade.sellOrderId(), 36).getBytes(StandardCharsets.UTF_8));
        buffer.putBytes(offset + 108, padRight(trade.buyAccountId(), 16).getBytes(StandardCharsets.UTF_8));
        buffer.putBytes(offset + 124, padRight(trade.sellAccountId(), 16).getBytes(StandardCharsets.UTF_8));
        buffer.putBytes(offset + 140, padRight(trade.symbol(), 8).getBytes(StandardCharsets.UTF_8));
        buffer.putLong(offset + 148, trade.quantity());
        buffer.putDouble(offset + 156, trade.price());
        buffer.putLong(offset + 164, trade.timestamp());
        buffer.putBytes(offset + 172, padRight(trade.currency(), 3).getBytes(StandardCharsets.UTF_8));
        return MESSAGE_LENGTH;
    }

    public static TradeEvent decode(DirectBuffer buffer, int offset) {
        String tradeId = buffer.getStringWithoutLengthUtf8(offset, 36).trim();
        String buyOrderId = buffer.getStringWithoutLengthUtf8(offset + 36, 36).trim();
        String sellOrderId = buffer.getStringWithoutLengthUtf8(offset + 72, 36).trim();
        String buyAccountId = buffer.getStringWithoutLengthUtf8(offset + 108, 16).trim();
        String sellAccountId = buffer.getStringWithoutLengthUtf8(offset + 124, 16).trim();
        String symbol = buffer.getStringWithoutLengthUtf8(offset + 140, 8).trim();
        long quantity = buffer.getLong(offset + 148);
        double price = buffer.getDouble(offset + 156);
        long timestamp = buffer.getLong(offset + 164);
        String currency = buffer.getStringWithoutLengthUtf8(offset + 172, 3).trim();

        return new TradeEvent(tradeId, buyOrderId, sellOrderId, buyAccountId, sellAccountId, symbol, quantity, price, timestamp, currency);
    }

    private static String padRight(String s, int length) {
        if (s == null) s = "";
        if (s.length() >= length) return s.substring(0, length);
        return String.format("%-" + length + "s", s);
    }
}
