package com.trading.settlement.clearing;

import com.trading.settlement.allocation.EquityTradeCaptureService.CapturedEquityTrade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CcpClearingEngineTest {

    @Test
    public void testTradeNovation() {
        CcpClearingEngine engine = new CcpClearingEngine();
        CapturedEquityTrade captured = new CapturedEquityTrade(
                "T-101", "B-1", "S-1", "MEM-BUYER", "MEM-SELLER", "GOOGL", 200, 150.00, 30000.0, "USD",
                LocalDate.now(), LocalDate.now().plusDays(1), "CAPTURED"
        );

        ClearedTrade cleared = engine.novateTrade(captured);
        assertNotNull(cleared);
        assertEquals("CLR-T-101", cleared.clearedTradeId());
        assertEquals("MEM-BUYER", cleared.clearingMemberBuyer());
        assertEquals("MEM-SELLER", cleared.clearingMemberSeller());
        assertEquals("NEXUS_CCP_01", cleared.ccpId());
    }

    @Test
    public void testMultilateralNettingCalculation() {
        CcpClearingEngine engine = new CcpClearingEngine();
        LocalDate date = LocalDate.now().plusDays(1);

        // Trade 1: MEMBER-A buys 100 GOOGL @ 150 from MEMBER-B
        ClearedTrade t1 = new ClearedTrade("CLR-1", "T-1", "MEMBER-A", "MEMBER-B", "CCP", "GOOGL", 100, 150.0, 15000.0, "USD", date, "NOVATED");
        // Trade 2: MEMBER-B buys 60 GOOGL @ 150 from MEMBER-A
        ClearedTrade t2 = new ClearedTrade("CLR-2", "T-2", "MEMBER-B", "MEMBER-A", "CCP", "GOOGL", 60, 150.0, 9000.0, "USD", date, "NOVATED");
        // Trade 3: MEMBER-A buys 50 GOOGL @ 150 from MEMBER-C
        ClearedTrade t3 = new ClearedTrade("CLR-3", "T-3", "MEMBER-A", "MEMBER-C", "CCP", "GOOGL", 50, 150.0, 7500.0, "USD", date, "NOVATED");

        List<NettedObligation> obligations = engine.calculateMultilateralNetting(List.of(t1, t2, t3));

        assertEquals(3, obligations.size());

        // MEMBER-A: Gross Buy = 150 shares ($22,500), Gross Sell = 60 shares ($9,000)
        // Net Shares = +90 (Receive 90 GOOGL), Net Cash = +$13,500 (Pay $13,500)
        NettedObligation obA = obligations.stream().filter(o -> o.clearingMemberId().equals("MEMBER-A")).findFirst().orElseThrow();
        assertEquals(90, obA.netQuantity());
        assertEquals(13500.0, obA.netCashAmount(), 0.01);

        // MEMBER-B: Gross Buy = 60 shares ($9,000), Gross Sell = 100 shares ($15,000)
        // Net Shares = -40 (Deliver 40 GOOGL), Net Cash = -$6,000 (Collect $6,000)
        NettedObligation obB = obligations.stream().filter(o -> o.clearingMemberId().equals("MEMBER-B")).findFirst().orElseThrow();
        assertEquals(-40, obB.netQuantity());
        assertEquals(-6000.0, obB.netCashAmount(), 0.01);
    }

    @Test
    public void testMarginCalculation() {
        CcpClearingEngine engine = new CcpClearingEngine();
        LocalDate date = LocalDate.now().plusDays(1);

        ClearedTrade t1 = new ClearedTrade("CLR-1", "T-1", "MEMBER-A", "MEMBER-B", "CCP", "AMZN", 100, 180.0, 18000.0, "USD", date, "NOVATED");

        // Price of AMZN drops by $10 -> buyer (MEMBER-A) faces variation margin obligation
        Map<String, Double> priceChanges = Map.of("AMZN", -10.0);

        CcpClearingEngine.MarginRequirement margin = engine.calculateMargin("MEMBER-A", List.of(t1), 0.10, priceChanges); // 10% IM

        assertEquals("MEMBER-A", margin.clearingMemberId());
        assertEquals(1800.0, margin.initialMarginRequired(), 0.01); // 10% of 18000
        assertEquals(1000.0, margin.variationMarginRequired(), 0.01); // 100 shares * $10 drop
        assertEquals(2800.0, margin.totalMarginRequired(), 0.01);
    }
}
