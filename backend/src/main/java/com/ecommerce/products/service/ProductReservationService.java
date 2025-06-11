package com.ecommerce.products.service;

import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.products.dto.ProductReservationDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductReservation;
import com.ecommerce.products.repository.ProductReservationRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReservationService {
    private final ProductReservationRepository productReservationRepository;
    private final ProductService productService;
    private final UserService userService;
    
    private static final int RESERVATION_DURATION_MINUTES = 30;
    
    @Transactional
    public ProductReservation reserveProduct(Long productId, Long userId, Integer quantity) {
        Product product = productService.getProductEntityById(productId);
        User user = userService.getUserById(userId);
        
        // Проверяем доступное количество
        int reservedQuantity = productReservationRepository.findByProductIdAndActiveTrue(productId)
            .stream()
            .mapToInt(ProductReservation::getQuantity)
            .sum();
            
        int availableQuantity = product.getStockQuantity() - reservedQuantity;
        if (availableQuantity < quantity) {
            throw new InsufficientStockException("Not enough stock available");
        }
        
        // Создаем резервирование
        ProductReservation reservation = new ProductReservation();
        reservation.setProduct(product);
        reservation.setUser(user);
        reservation.setQuantity(quantity);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(RESERVATION_DURATION_MINUTES));
        reservation.setActive(true);
        
        return productReservationRepository.save(reservation);
    }
    
    @Transactional
    public ProductReservationDto createReservation(Long productId, Integer quantity) {
        User user = userService.getUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        ProductReservation reservation = reserveProduct(productId, user.getId(), quantity);
        return convertToDto(reservation);
    }
    
    @Transactional
    public void releaseReservation(Long reservationId) {
        ProductReservation reservation = productReservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
            
        if (reservation.isActive()) {
            reservation.setActive(false);
            productReservationRepository.save(reservation);
        }
    }
    
    @Transactional
    public void releaseReservationsForOrder(Product product, User user) {
        productReservationRepository.deactivateReservationsForUser(product, user);
    }
    
    @Scheduled(fixedRate = 6000000) // Каждый час
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        productReservationRepository.deactivateExpiredReservations(now);
    }
    
    public List<ProductReservation> getUserReservations(Long userId) {
        return productReservationRepository.findByUserIdAndActiveTrue(userId);
    }
    
    public List<ProductReservationDto> getUserReservations() {
        User user = userService.getUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        return getUserReservations(user.getId()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ProductReservation> getProductReservations(Long productId) {
        return productReservationRepository.findByProductIdAndActiveTrue(productId);
    }
    
    public ProductReservation getReservationById(Long reservationId) {
        return productReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }
    
    public List<ProductReservationDto> getProductReservationsDto(Long productId) {
        return getProductReservations(productId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private ProductReservationDto convertToDto(ProductReservation reservation) {
        ProductReservationDto dto = new ProductReservationDto();
        dto.setId(reservation.getId());
        dto.setProductId(reservation.getProduct().getId());
        dto.setUserId(reservation.getUser().getId());
        dto.setQuantity(reservation.getQuantity());
        dto.setReservedAt(reservation.getReservedAt());
        dto.setExpiresAt(reservation.getExpiresAt());
        dto.setActive(reservation.isActive());
        return dto;
    }
} 