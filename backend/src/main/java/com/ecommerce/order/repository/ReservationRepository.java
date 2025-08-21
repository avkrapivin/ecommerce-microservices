package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    Optional<Reservation> findById(Long id);
    
    Optional<Reservation> findByReservationNumber(String reservationNumber);
    
    List<Reservation> findByEmail(String email);
    
    List<Reservation> findByStatus(String status);
    
    @Query("SELECT r FROM Reservation r WHERE r.expiresAt < :now AND r.status = 'ACTIVE'")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM Reservation r WHERE r.email = :email AND r.status = 'ACTIVE'")
    List<Reservation> findActiveReservationsByEmail(@Param("email") String email);
}

