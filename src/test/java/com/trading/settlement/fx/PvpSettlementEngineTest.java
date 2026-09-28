package com.trading.settlement.fx;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PvpSettlementEngineTest {

    private PvpSettlementEngine pvpEngine;

    @BeforeEach
    public void setUp() {
        pvpEngine = new PvpSettlementEngine();
    }

    @Test
    public void testPvpSettlementLifecycle() {
        var instruction = pvpEngine.createPvPInstruction(
                "FXTRD-9988",
                "BUYER-BANK",
                "SELLER-BANK",
                "EUR",
                1_000_000.0,
                "USD",
                1_085_000.0,
                LocalDate.now().plusDays(2)
        );

        assertNotNull(instruction);
        assertEquals("PENDING_MATCH", instruction.status());

        boolean result = pvpEngine.executePvPSettlement(instruction.instructionId());
        assertTrue(result);
        assertEquals("SETTLED_PVP_CLS", pvpEngine.getInstruction(instruction.instructionId()).status());
    }

    @Test
    public void testGenerateSwiftMT300() {
        var instruction = pvpEngine.createPvPInstruction(
                "FXTRD-7711",
                "BANK-A",
                "BANK-B",
                "GBP",
                500_000.0,
                "USD",
                647_500.0,
                LocalDate.now().plusDays(2)
        );

        String swiftMessage = pvpEngine.generateSwiftMT300(instruction);
        assertNotNull(swiftMessage);
        assertTrue(swiftMessage.contains(":20C::SEME//"));
        assertTrue(swiftMessage.contains("GBP500000.00"));
        assertTrue(swiftMessage.contains("USD647500.00"));
    }
}
