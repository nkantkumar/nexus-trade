package com.trading.settlement.allocation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BlockTradeAllocationEngineTest {

    @Test
    public void testBlockTradeAllocationSuccess() {
        BlockTradeAllocationEngine engine = new BlockTradeAllocationEngine();

        Map<String, Double> pctMap = new LinkedHashMap<>();
        pctMap.put("FUND-A", 40.0);
        pctMap.put("FUND-B", 35.0);
        pctMap.put("FUND-C", 25.0);

        Map<String, String> custodianMap = Map.of(
                "FUND-A", "DTCC",
                "FUND-B", "EUROCLEAR",
                "FUND-C", "CLEARSTREAM"
        );

        AllocationInstruction instruction = engine.allocateBlockTrade(
                "BLK-1001",
                "TSLA",
                "BUY",
                10000,
                250.00,
                "USD",
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                pctMap,
                custodianMap
        );

        assertEquals("BLK-1001", instruction.getBlockTradeId());
        assertEquals("TSLA", instruction.getSymbol());
        assertEquals(3, instruction.getAllocations().size());

        // Check allocation share quantities: 4000, 3500, 2500
        assertEquals(4000, instruction.getAllocations().get(0).allocatedQuantity());
        assertEquals(3500, instruction.getAllocations().get(1).allocatedQuantity());
        assertEquals(2500, instruction.getAllocations().get(2).allocatedQuantity());

        long sumShares = instruction.getAllocations().stream().mapToLong(AllocationInstruction.AccountAllocation::allocatedQuantity).sum();
        assertEquals(10000, sumShares);

        assertEquals(1000000.0, instruction.getAllocations().get(0).allocatedAmount(), 0.01);
        assertEquals("DTCC", instruction.getAllocations().get(0).custodianAccountId());
    }

    @Test
    public void testInvalidPercentageSumThrowsException() {
        BlockTradeAllocationEngine engine = new BlockTradeAllocationEngine();
        Map<String, Double> pctMap = Map.of("FUND-A", 50.0, "FUND-B", 40.0); // 90% total

        assertThrows(IllegalArgumentException.class, () ->
                engine.allocateBlockTrade("BLK-FAIL", "NVDA", "BUY", 1000, 100.0, "USD",
                        LocalDate.now(), LocalDate.now().plusDays(1), pctMap, Map.of())
        );
    }
}
