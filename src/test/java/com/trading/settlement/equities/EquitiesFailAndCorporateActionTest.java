package com.trading.settlement.equities;

import com.trading.settlement.dvp.DvPModel;
import com.trading.settlement.dvp.SettlementInstruction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EquitiesFailAndCorporateActionTest {

    @Test
    public void testStockSplitAdjustment() {
        CorporateActionAdjuster adjuster = new CorporateActionAdjuster();

        CorporateAction split = new CorporateAction(
                "CA-SPLIT-01", "AAPL", CorporateAction.Type.STOCK_SPLIT,
                LocalDate.now(), LocalDate.now(), LocalDate.now(), 2.0, 1.0, 0.0, "USD"
        );

        SettlementInstruction instr = new SettlementInstruction(
                "INSTR-AAPL-1", "OBLIG-1", "SELLER", "BUYER", "AAPL", 100, 18000.0, "USD", LocalDate.now(), DvPModel.MODEL_1
        );

        List<CorporateActionAdjuster.AdjustedInstruction> adjusted = adjuster.applyCorporateAction(split, List.of(instr));

        assertEquals(1, adjusted.size());
        assertEquals("AAPL", adjusted.get(0).symbol());
        assertEquals(100, adjusted.get(0).originalQuantity());
        assertEquals(200, adjusted.get(0).adjustedQuantity()); // 2-for-1 split doubles quantity
        assertEquals(18000.0, adjusted.get(0).adjustedCashAmount()); // Total cash value unchanged
    }

    @Test
    public void testCashDividendAdjustment() {
        CorporateActionAdjuster adjuster = new CorporateActionAdjuster();

        CorporateAction dividend = new CorporateAction(
                "CA-DIV-01", "JPM", CorporateAction.Type.CASH_DIVIDEND,
                LocalDate.now(), LocalDate.now(), LocalDate.now(), 0.0, 0.0, 1.15, "USD"
        );

        SettlementInstruction instr = new SettlementInstruction(
                "INSTR-JPM-1", "OBLIG-2", "SELLER", "BUYER", "JPM", 1000, 200000.0, "USD", LocalDate.now(), DvPModel.MODEL_1
        );

        List<CorporateActionAdjuster.AdjustedInstruction> adjusted = adjuster.applyCorporateAction(dividend, List.of(instr));

        assertEquals(1, adjusted.size());
        assertEquals(200000.0 + 1150.0, adjusted.get(0).adjustedCashAmount(), 0.01); // $1.15 * 1000 shares dividend claim added
    }

    @Test
    public void testBuyInTriggerOnShortage() {
        EquityFailManagementEngine failEngine = new EquityFailManagementEngine();

        SettlementInstruction failedInstr = new SettlementInstruction(
                "INSTR-FAIL-01", "OBLIG-FAIL", "SELLER-FAIL", "BUYER-1", "TSLA", 50, 12500.0, "USD", LocalDate.now(), DvPModel.MODEL_1
        );
        failedInstr.setStatus(SettlementInstruction.Status.FAILED_SECURITIES_SHORTAGE);

        EquityFailManagementEngine.BuyInNotice buyIn = failEngine.triggerBuyIn(failedInstr, 260.0, 5.0); // 5% penalty rate

        assertNotNull(buyIn);
        assertEquals("SELLER-FAIL", buyIn.failingAccountId());
        assertEquals(50, buyIn.shortQuantity());
        // 50 * 260 = 13000 + 5% penalty (650) = 13650
        assertEquals(13650.0, buyIn.totalPenaltyCost(), 0.01);
    }
}
