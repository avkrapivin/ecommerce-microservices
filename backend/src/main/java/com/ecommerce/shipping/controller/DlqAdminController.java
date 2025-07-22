package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.listener.DlqProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin контроллер для управления Dead Letter Queue
 * Доступен только администраторам
 */
@RestController
@RequestMapping("/admin/dlq")
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DlqAdminController {

    private final DlqProcessor dlqProcessor;

    /**
     * Получить количество сообщений в DLQ
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDlqCount() {
        try {
            int messageCount = dlqProcessor.getDlqMessageCount();
            
            return ResponseEntity.ok(Map.of(
                "dlqMessageCount", messageCount,
                "status", messageCount > 0 ? "WARNING" : "OK",
                "message", messageCount > 0 
                    ? String.format("Found %d messages in DLQ!", messageCount)
                    : "DLQ is empty"
            ));
            
        } catch (Exception e) {
            log.error("Error getting DLQ count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get DLQ count",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Ручная повторная обработка сообщения из DLQ
     */
    @PostMapping("/reprocess/{messageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reprocessMessage(@PathVariable String messageId) {
        try {
            log.info("Admin requested reprocessing of DLQ message: {}", messageId);
            
            boolean success = dlqProcessor.reprocessDlqMessage(messageId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Message %s successfully reprocessed", messageId)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", String.format("Failed to reprocess message %s", messageId)
                ));
            }
            
        } catch (Exception e) {
            log.error("Error reprocessing DLQ message {}: {}", messageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to reprocess message",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Переотправить сообщение из DLQ обратно в основную очередь
     */
    @PostMapping("/requeue/{messageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> requeueMessage(@PathVariable String messageId) {
        try {
            log.info("Admin requested requeuing of DLQ message: {}", messageId);
            
            boolean success = dlqProcessor.requeueDlqMessage(messageId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Message %s successfully requeued to main queue", messageId)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", String.format("Failed to requeue message %s", messageId)
                ));
            }
            
        } catch (Exception e) {
            log.error("Error requeuing DLQ message {}: {}", messageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to requeue message",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Принудительная проверка DLQ на наличие сообщений
     */
    @PostMapping("/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkDlq() {
        try {
            log.info("Admin requested manual DLQ check");
            
            // Trigger manual check
            dlqProcessor.checkDlqMessages();
            
            int messageCount = dlqProcessor.getDlqMessageCount();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "DLQ check completed",
                "dlqMessageCount", messageCount
            ));
            
        } catch (Exception e) {
            log.error("Error during manual DLQ check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to check DLQ",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Получить статус DLQ системы
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDlqStatus() {
        try {
            int messageCount = dlqProcessor.getDlqMessageCount();
            
            String status = messageCount == 0 ? "HEALTHY" : 
                           messageCount < 10 ? "WARNING" : "CRITICAL";
            
            return ResponseEntity.ok(Map.of(
                "dlqMessageCount", messageCount,
                "status", status,
                "healthCheck", messageCount == 0,
                "lastCheck", System.currentTimeMillis(),
                "recommendations", messageCount > 0 ? 
                    "Check logs for errors and consider reprocessing messages" :
                    "DLQ is empty - system operating normally"
            ));
            
        } catch (Exception e) {
            log.error("Error getting DLQ status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "healthCheck", false,
                "error", e.getMessage()
            ));
        }
    }
} 