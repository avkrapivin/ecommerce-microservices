package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.dto.PayPalPaymentResponse;
import com.ecommerce.payment.service.PayPalService;
import com.ecommerce.products.dto.UpdateProductDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.service.ProductService;
import com.ecommerce.products.service.ProductReservationService;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.OrderStatusException;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final ProductService productService;
    private final OrderCalculationService calculationService;
    private final ProductReservationService productReservationService;
    private final PayPalService payPalService;

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#orderId")
    public OrderDto getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#orderNumber")
    public OrderDto getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userOrders", key = "#userId")
    public List<OrderDto> getUserOrders(Long userId) {
        User user = userService.getUserById(userId);
        return orderRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "userOrders", key = "#userId")
    public OrderDto createOrder(Long userId, OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderStatusException("Order must contain at least one item");
        }

        User user = userService.getUserById(userId);
        
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress(convertToEntity(request.getShippingAddress()));
        
        // Создаем элементы заказа
        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() <= 0) {
                throw new OrderStatusException("Item quantity must be positive");
            }

            Product product = productService.getProductEntityById(itemRequest.getProduct().getId());
            
            if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderStatusException("Product price must be positive");
            }
            
            // Проверяем резервирование
            productReservationService.reserveProduct(product.getId(), userId, itemRequest.getQuantity());
            
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            
            order.addItem(orderItem);
        }
        
        // Сохраняем заказ, чтобы сработал @PrePersist и установились базовые значения
        Order savedOrder = orderRepository.save(order);
        
        // Обновляем значения после сохранения
        BigDecimal subtotal = savedOrder.getSubtotal();
        BigDecimal shippingCost = calculationService.calculateShippingCost(subtotal);
        BigDecimal tax = calculationService.calculateTax(subtotal);
        BigDecimal total = calculationService.calculateTotal(subtotal, shippingCost, tax);
        
        savedOrder.setSubtotal(subtotal);
        savedOrder.setShippingCost(shippingCost);
        savedOrder.setTax(tax);
        savedOrder.setTotal(total);
        
        // Сохраняем обновленные значения
        savedOrder = orderRepository.save(savedOrder);
        
        return convertToDto(savedOrder);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderDto updateOrderStatus(Long orderId, UpdateOrderStatusDto updateDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        // Если заказ переходит в статус CONFIRMED, списываем товары с остатка
        if (updateDto.getStatus() == OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                int newQuantity = product.getStockQuantity() - item.getQuantity();
                if (newQuantity < 0) {
                    throw new OrderStatusException("Not enough stock for product: " + product.getName());
                }
                
                // Создаем DTO для обновления
                UpdateProductDto updateProductDto = new UpdateProductDto();
                updateProductDto.setStockQuantity(newQuantity);
                productService.updateProduct(product.getId(), updateProductDto);
                
                // Освобождаем резервирование после списания
                productReservationService.releaseReservationsForOrder(product, order.getUser());
            }
        }
        
        order.setStatus(updateDto.getStatus());
        if (updateDto.getPaymentStatus() != null) {
            order.setPaymentStatus(updateDto.getPaymentStatus());
        }
        if (updateDto.getTrackingNumber() != null) {
            order.setTrackingNumber(updateDto.getTrackingNumber());
        }
        
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStatusException("Only pending orders can be cancelled");
        }
        
        // Освобождаем резервирования для каждого товара в заказе
        for (OrderItem item : order.getItems()) {
            productReservationService.releaseReservationsForOrder(item.getProduct(), order.getUser());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public PayPalPaymentResponse createPayment(Long orderId) throws PayPalRESTException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new OrderStatusException("Order is not in pending payment status");
        }

        if (order.getPaymentId() != null) {
            throw new OrderStatusException("Payment already initiated for this order");
        }

        Payment payment = payPalService.createPayment(
                order.getTotal().doubleValue(),
                "USD",
                "paypal",
                "sale",
                "Order #" + order.getOrderNumber(),
                "http://localhost:3000/orders/payment/cancel",
                "http://localhost:3000/orders/payment/success"
        );

        // Сохраняем информацию о платеже
        order.setPaymentId(payment.getId());
        order.setPaymentMethod("PAYPAL");
        order.setPaymentStatus(PaymentStatus.PROCESSING);
        orderRepository.save(order);

        return PayPalPaymentResponse.success(payment);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public void processSuccessfulPayment(String paymentId, String payerId) throws PayPalRESTException {
        Order order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for payment: " + paymentId));

        Payment payment = payPalService.executePayment(paymentId, payerId);
        
        if (payment.getState().equals("approved")) {
            order.setPayerId(payerId);
            order.setPaymentDate(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setStatus(OrderStatus.CONFIRMED);
            
            // Списываем товары с остатка
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                int newQuantity = product.getStockQuantity() - item.getQuantity();
                if (newQuantity < 0) {
                    throw new OrderStatusException("Not enough stock for product: " + product.getName());
                }
                
                UpdateProductDto updateProductDto = new UpdateProductDto();
                updateProductDto.setStockQuantity(newQuantity);
                productService.updateProduct(product.getId(), updateProductDto);
                
                productReservationService.releaseReservationsForOrder(product, order.getUser());
            }
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentError("Payment was not approved: " + payment.getState());
        }
        
        orderRepository.save(order);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public void handlePaymentFailure(String paymentId, String errorMessage) {
        Order order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for payment: " + paymentId));

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setPaymentError(errorMessage);
        orderRepository.save(order);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public void handlePaymentCancellation(String paymentId) {
        Order order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for payment: " + paymentId));

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setPaymentError("Payment was cancelled by user");
        orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setShippingAddress(convertToDto(order.getShippingAddress()));
        dto.setItems(order.getItems().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
        dto.setSubtotal(order.getSubtotal());
        dto.setShippingCost(order.getShippingCost());
        dto.setTax(order.getTax());
        dto.setTotal(order.getTotal());
        dto.setPaymentId(order.getPaymentId());
        dto.setPayerId(order.getPayerId());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentDate(order.getPaymentDate());
        dto.setPaymentError(order.getPaymentError());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    private OrderItemDto convertToDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setProduct(productService.convertToDto(item.getProduct()));
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }

    private ShippingAddressDto convertToDto(ShippingAddress address) {
        ShippingAddressDto dto = new ShippingAddressDto();
        dto.setId(address.getId());
        dto.setFirstName(address.getFirstName());
        dto.setLastName(address.getLastName());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        dto.setPhoneNumber(address.getPhoneNumber());
        dto.setEmail(address.getEmail());
        return dto;
    }

    private ShippingAddress convertToEntity(ShippingAddressRequest request) {
        ShippingAddress address = new ShippingAddress();
        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setEmail(request.getEmail());
        return address;
    }
} 