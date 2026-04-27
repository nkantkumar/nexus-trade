package com.trading.settlement;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import quickfix.Message;

@Service
public class SettlementService {
    private final KafkaTemplate<String, SettlementInstruction> kafkaTemplate;
    private final Map<String, DvpState> states = new ConcurrentHashMap<>();

    public SettlementService(KafkaTemplate<String, SettlementInstruction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void onExecutionReport(ExecutionReport report) {
        states.put(report.executionId(), DvpState.INITIATED);
        String pacs008 = """
                <Document><FIToFICstmrCdtTrf><GrpHdr><MsgId>%s</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>
                """.formatted(report.executionId());
        kafkaTemplate.send("settlement-instructions", report.executionId(),
                new SettlementInstruction(report.executionId(), pacs008, Instant.now()));
        states.put(report.executionId(), DvpState.SETTLED);
    }

    public void parseFixExecution(Message message) {
        // quickfix message parsing extension point
    }

    @Scheduled(fixedDelay = 300_000)
    public void reconcileCustodianRecords() {
        // reconciliation against custodian feed should run here
    }
}

enum DvpState { INITIATED, PENDING_CASH, PENDING_SECURITIES, SETTLED, FAILED }
record ExecutionReport(String executionId, String accountId, double amount) {}
record SettlementInstruction(String executionId, String iso20022Payload, Instant ts) {}
