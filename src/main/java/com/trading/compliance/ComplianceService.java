package com.trading.compliance;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.trading.infra.chronicle.ChronicleAppenders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ComplianceService {
    private static final String COMPLIANCE_CHRONICLE_PATH = "build/chronicle/compliance";

    private final Map<String, Instant> largeOrders = new ConcurrentHashMap<>();
    private final RestClient restClient = RestClient.create("https://ofac.example");

    @KafkaListener(topics = "order-events", groupId = "compliance")
    public void onOrderEvent(ComplianceOrderEvent event) {
        if (event.quantity() > 100_000) {
            largeOrders.put(event.orderId(), event.ts());
        }
        if ("CANCELLED".equals(event.state()) && isSpoofing(event)) {
            ChronicleAppenders.forPath(COMPLIANCE_CHRONICLE_PATH).writeText("SPOOFING_ALERT:" + event.orderId());
        }
    }

    @KafkaListener(topics = "execution-events", groupId = "compliance")
    public void onExecution(ComplianceExecutionEvent event) {
        if (event.buyAccount().equals(event.sellAccount())) {
            ChronicleAppenders.forPath(COMPLIANCE_CHRONICLE_PATH).writeText("WASH_TRADE_ALERT:" + event.executionId());
        }
        if (event.notional() > 1_000_000) {
            ChronicleAppenders.forPath(COMPLIANCE_CHRONICLE_PATH).writeText("AML_THRESHOLD_ALERT:" + event.executionId());
        }
        if (isSanctioned(event.buyAccount()) || isSanctioned(event.sellAccount())) {
            ChronicleAppenders.forPath(COMPLIANCE_CHRONICLE_PATH).writeText("OFAC_ALERT:" + event.executionId());
        }
    }

    private boolean isSpoofing(ComplianceOrderEvent event) {
        Instant placedAt = largeOrders.get(event.orderId());
        return placedAt != null && Duration.between(placedAt, event.ts()).toSeconds() < 60;
    }

    @CircuitBreaker(name = "ofac", fallbackMethod = "fallback")
    public boolean isSanctioned(String accountId) {
        String body = restClient.get().uri("/screen/{id}", accountId).retrieve().body(String.class);
        return "MATCH".equals(body);
    }

    private boolean fallback(String accountId, Throwable throwable) {
        return false;
    }
}

record ComplianceOrderEvent(String orderId, String state, long quantity, Instant ts) {}
record ComplianceExecutionEvent(String executionId, String buyAccount, String sellAccount, double notional) {}
