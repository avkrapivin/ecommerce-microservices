package com.ecommerce.products.controller;

import com.ecommerce.products.dto.CreateReservationRequest;
import com.ecommerce.products.dto.ProductReservationDto;
import com.ecommerce.products.entity.ProductReservation;
import com.ecommerce.products.service.ProductReservationService;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ProductReservationController {

    private final ProductReservationService productReservationService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductReservationDto> createReservation(
            @RequestBody CreateReservationRequest request) {
        return ResponseEntity.ok(productReservationService.createReservation(
                request.getProductId(), 
                request.getQuantity()));
    }

    @DeleteMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseReservation(@PathVariable Long reservationId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getUserByEmail(currentUserEmail);
        
        ProductReservation reservation = productReservationService.getReservationById(reservationId);
        if (!reservation.getUser().getId().equals(currentUser.getId()) && 
            !SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }
        
        productReservationService.releaseReservation(reservationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductReservationDto>> getUserReservations() {
        return ResponseEntity.ok(productReservationService.getUserReservations());
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductReservationDto>> getProductReservations(@PathVariable Long productId) {
        return ResponseEntity.ok(productReservationService.getProductReservationsDto(productId));
    }
} 