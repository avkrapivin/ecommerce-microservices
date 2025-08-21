package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CreateReservationRequest;
import com.ecommerce.order.dto.ReservationDto;
import com.ecommerce.order.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order-reservations")
@RequiredArgsConstructor
@Slf4j
public class OrderReservationController {
    
    private final ReservationService reservationService;
    
    @PostMapping
    public ResponseEntity<ReservationDto> createReservation(@RequestBody CreateReservationRequest request) {
        log.info("Creating reservation for email: {}, totalAmount: {}", request.getEmail(), request.getTotalAmount());
        
        try {
            ReservationDto reservation = reservationService.createReservation(request.getEmail(), request.getTotalAmount());
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            log.error("Error creating reservation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user")
    public ResponseEntity<List<ReservationDto>> getUserReservations(@RequestParam String email) {
        log.info("Getting active reservations for user: {}", email);
        
        try {
            List<ReservationDto> reservations = reservationService.findActiveReservationsByEmail(email);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            log.error("Error getting user reservations: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{email}")
    public ResponseEntity<List<ReservationDto>> getReservationsByEmail(@PathVariable String email) {
        log.info("Getting all reservations for user: {}", email);
        
        try {
            List<ReservationDto> reservations = reservationService.findByEmail(email);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            log.error("Error getting reservations by email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> getReservationById(@PathVariable Long id) {
        log.info("Getting reservation by id: {}", id);
        
        return reservationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/number/{reservationNumber}")
    public ResponseEntity<ReservationDto> getReservationByNumber(@PathVariable String reservationNumber) {
        log.info("Getting reservation by number: {}", reservationNumber);
        
        return reservationService.findByReservationNumber(reservationNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{reservationNumber}/confirm")
    public ResponseEntity<ReservationDto> confirmReservation(@PathVariable String reservationNumber) {
        log.info("Confirming reservation: {}", reservationNumber);
        
        try {
            ReservationDto reservation = reservationService.confirmReservation(reservationNumber);
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            log.error("Error confirming reservation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{reservationNumber}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable String reservationNumber) {
        log.info("Cancelling reservation: {}", reservationNumber);
        
        try {
            reservationService.cancelReservation(reservationNumber);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cancelling reservation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}

