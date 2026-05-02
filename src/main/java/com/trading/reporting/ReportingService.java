package com.trading.reporting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.trading.infra.chronicle.ChronicleAppenders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportingService {
    private static final String REPORTING_CHRONICLE_PATH = "build/chronicle/reporting";

    private final List<DomainEvent> events = new ArrayList<>();

    @KafkaListener(topics = {"order-events", "execution-events", "risk-events"}, groupId = "reporting")
    public void onDomainEvent(DomainEvent event) {
        events.add(event);
        ChronicleAppenders.forPath(REPORTING_CHRONICLE_PATH).writeText(event.toString());
    }

    @GetMapping("/reports/mifid")
    public String mifidCsv() {
        return "event_id,event_type,timestamp\n" + events.stream()
                .map(e -> e.id() + "," + e.type() + "," + e.ts())
                .collect(Collectors.joining("\n"));
    }

    @GetMapping("/reports/trades")
    public List<DomainEvent> tradeHistory(@RequestParam int page, @RequestParam int size) {
        int from = Math.min(page * size, events.size());
        int to = Math.min(from + size, events.size());
        return events.subList(from, to);
    }

    public List<DomainEvent> replayFrom(Instant from) {
        return events.stream().filter(e -> e.ts().isAfter(from)).toList();
    }
}

record DomainEvent(String id, String type, Instant ts, String payload) {}
