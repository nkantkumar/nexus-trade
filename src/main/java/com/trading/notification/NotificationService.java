package com.trading.notification;

import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public NotificationService(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTradeConfirmation(String accountId, String message) {
        kafkaTemplate.send("notifications", accountId,
                new NotificationEvent(accountId, "TRADE_CONFIRMATION", message, Instant.now()));
    }

    public void sendMarginAlert(String accountId, String message) {
        kafkaTemplate.send("notifications", accountId,
                new NotificationEvent(accountId, "MARGIN_ALERT", message, Instant.now()));
    }

    public void sendOtp(String accountId, String otpCode) {
        kafkaTemplate.send("notifications", accountId,
                new NotificationEvent(accountId, "OTP", "Your OTP is " + otpCode, Instant.now()));
    }

    public void sendRiskAlert(String accountId, String message) {
        kafkaTemplate.send("notifications", accountId,
                new NotificationEvent(accountId, "RISK_ALERT", message, Instant.now()));
    }
}

record NotificationEvent(String accountId, String type, String message, Instant ts) {}
