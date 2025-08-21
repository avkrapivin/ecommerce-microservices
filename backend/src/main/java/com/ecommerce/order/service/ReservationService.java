package com.ecommerce.order.service;

import com.ecommerce.order.dto.ReservationDto;
import com.ecommerce.order.entity.Reservation;
import com.ecommerce.order.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    
    @Transactional(readOnly = true)
    public Optional<ReservationDto> findById(Long id) {
        log.debug("Finding reservation by id: {}", id);
        return reservationRepository.findById(id)
                .map(this::convertToDto);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationDto> findByEmail(String email) {
        log.debug("Finding reservations by email: {}", email);
        return reservationRepository.findByEmail(email)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Optional<ReservationDto> findByReservationNumber(String reservationNumber) {
        log.debug("Finding reservation by number: {}", reservationNumber);
        return reservationRepository.findByReservationNumber(reservationNumber)
                .map(this::convertToDto);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationDto> findActiveReservationsByEmail(String email) {
        log.debug("Finding active reservations by email: {}", email);
        return reservationRepository.findActiveReservationsByEmail(email)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ReservationDto createReservation(String email, BigDecimal totalAmount) {
        log.debug("Creating reservation for email: {}, totalAmount: {}", email, totalAmount);
        
        String reservationNumber = generateReservationNumber();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15); // 15 минут на оплату
        
        Reservation reservation = Reservation.builder()
                .email(email)
                .reservationNumber(reservationNumber)
                .status("ACTIVE")
                .totalAmount(totalAmount)
                .expiresAt(expiresAt)
                .build();
        
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Created reservation: {} for email: {}", reservationNumber, email);
        
        return convertToDto(savedReservation);
    }
    
    @Transactional
    public ReservationDto confirmReservation(String reservationNumber) {
        log.debug("Confirming reservation: {}", reservationNumber);
        
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationNumber));
        
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new RuntimeException("Reservation is not active: " + reservationNumber);
        }
        
        if (LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            throw new RuntimeException("Reservation has expired: " + reservationNumber);
        }
        
        reservation.setStatus("CONFIRMED");
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Confirmed reservation: {}", reservationNumber);
        
        return convertToDto(savedReservation);
    }
    
    @Transactional
    public void cancelReservation(String reservationNumber) {
        log.debug("Cancelling reservation: {}", reservationNumber);
        
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationNumber));
        
        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);
        log.info("Cancelled reservation: {}", reservationNumber);
    }
    
    @Transactional
    public void cleanupExpiredReservations() {
        log.debug("Cleaning up expired reservations");
        
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(LocalDateTime.now());
        
        for (Reservation reservation : expiredReservations) {
            reservation.setStatus("EXPIRED");
            reservationRepository.save(reservation);
            log.debug("Marked reservation as expired: {}", reservation.getReservationNumber());
        }
        
        log.info("Cleaned up {} expired reservations", expiredReservations.size());
    }
    
    private String generateReservationNumber() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private ReservationDto convertToDto(Reservation reservation) {
        return ReservationDto.builder()
                .id(reservation.getId())
                .email(reservation.getEmail())
                .reservationNumber(reservation.getReservationNumber())
                .status(reservation.getStatus())
                .totalAmount(reservation.getTotalAmount())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}

