package com.trading.fx;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FxTradingServiceTest {

    private FxTradingService fxService;

    @BeforeEach
    public void setUp() {
        fxService = new FxTradingService(null);
    }

    @Test
    public void testGetQuoteAndPips() {
        FxQuote quote = fxService.getQuote("EURUSD");
        assertNotNull(quote);
        assertEquals("EURUSD", quote.symbol());
        assertTrue(quote.ask() >= quote.bid());
        assertTrue(quote.spreadPips() >= 0);
    }

    @Test
    public void testForwardQuoteCalculation() {
        Map<String, Object> forwardQuote = fxService.getForwardQuote("EURUSD", 0.03, 0.05, 90);
        assertNotNull(forwardQuote);
        double spotMid = (Double) forwardQuote.get("spotMid");
        double forwardRate = (Double) forwardQuote.get("forwardRate");
        assertTrue(forwardRate > spotMid, "Forward rate should be higher when quote currency interest rate is higher");
    }

    @Test
    public void testSpotTradeExecution() {
        FxTradeRequest req = new FxTradeRequest(
                "ACC-1001",
                "EURUSD",
                FxOrderType.SPOT,
                "BUY",
                2.5,
                0,
                LocalDate.now().plusDays(2),
                0.0,
                0.0,
                "CITI_LP"
        );

        FxTrade trade = fxService.executeTrade(req);
        assertNotNull(trade);
        assertTrue(trade.tradeId().startsWith("FXTRD-"));
        assertEquals("EURUSD", trade.symbol());
        assertEquals("BUY", trade.side());
        assertEquals(2.5, trade.lots());
        assertEquals(250_000, trade.units());
        assertEquals("EUR", trade.baseCurrency());
        assertEquals("USD", trade.quoteCurrency());
    }

    @Test
    public void testForwardTradeExecution() {
        FxTradeRequest req = new FxTradeRequest(
                "ACC-1001",
                "USDJPY",
                FxOrderType.FORWARD,
                "BUY",
                1.0,
                30,
                LocalDate.now().plusDays(30),
                0.05,
                0.005,
                "BARCLAYS_LP"
        );

        FxTrade trade = fxService.executeTrade(req);
        assertNotNull(trade);
        assertEquals(FxOrderType.FORWARD, trade.fxOrderType());
        assertEquals(30, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), trade.valueDate()));
    }

    @Test
    public void testTriangularArbitrageCheck() {
        Map<String, Object> res = fxService.checkTriangularArbitrage("EURUSD", "USDJPY", "EURJPY");
        assertNotNull(res);
        assertTrue(res.containsKey("discrepancyPips"));
        assertTrue(res.containsKey("syntheticCross"));
    }
}
