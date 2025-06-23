package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
@Slf4j
public class SnsWebhookController {

    private final OrderStatusUpdateListener orderStatusUpdateListener;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleSnsWebhook(@RequestBody String payload, 
                                                   @RequestHeader(value = "x-amz-sns-message-type", required = false) String messageType) {
        try {
            log.info("Received SNS webhook with message type: {}", messageType);
            
            if ("SubscriptionConfirmation".equals(messageType)) {
                // Подтверждаем подписку на SNS топик
                JsonNode jsonNode = objectMapper.readTree(payload);
                String subscribeUrl = jsonNode.get("SubscribeURL").asText();
                log.info("Confirming SNS subscription: {}", subscribeUrl);
                // Здесь можно добавить HTTP клиент для подтверждения подписки
                return ResponseEntity.ok("Subscription confirmed");
            } else if ("Notification".equals(messageType)) {
                // Обрабатываем уведомление
                JsonNode jsonNode = objectMapper.readTree(payload);
                String message = jsonNode.get("Message").asText();
                String subject = jsonNode.has("Subject") ? jsonNode.get("Subject").asText() : null;
                
                log.info("Processing SNS notification with subject: {}", subject);
                
                if ("OrderStatusUpdated".equals(subject)) {
                    orderStatusUpdateListener.handleOrderStatusUpdate(message);
                }
                
                return ResponseEntity.ok("Notification processed");
            }
            
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            log.error("Error processing SNS webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }
} 