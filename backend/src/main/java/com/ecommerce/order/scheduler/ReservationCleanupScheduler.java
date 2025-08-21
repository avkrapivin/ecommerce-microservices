package com.ecommerce.order.scheduler;

import com.ecommerce.order.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupScheduler {
    
    private final ReservationService reservationService;
    
    /**
     * Запускается каждые 5 минут для очистки истекших резервирований
     */
    @Scheduled(fixedRate = 300000) // 5 минут = 300000 миллисекунд
    public void cleanupExpiredReservations() {
        log.debug("Starting scheduled cleanup of expired reservations");
        
        try {
            reservationService.cleanupExpiredReservations();
            log.debug("Scheduled cleanup of expired reservations completed");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of expired reservations: {}", e.getMessage(), e);
        }
    }
}
