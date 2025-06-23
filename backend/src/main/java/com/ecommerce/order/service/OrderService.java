package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.products.dto.UpdateProductDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.service.ProductService;
import com.ecommerce.products.service.ProductReservationService;
import com.ecommerce.shipping.service.ShippingService;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.OrderStatusException;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final OrderEventPublisher orderEventPublisher;
    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
        BigDecimal shippingCost = calculationService.calculateShippingCost(savedOrder);
        BigDecimal tax = calculationService.calculateTax(savedOrder);
        BigDecimal total = calculationService.calculateTotal(savedOrder, shippingCost, tax);

        savedOrder.setShippingCost(shippingCost);
        savedOrder.setTax(tax);
        savedOrder.setTotal(total);

        // Сохраняем обновленные значения
        savedOrder = orderRepository.save(savedOrder);

        // Создаем запись о доставке
        shippingService.createShippingInfo(savedOrder.getOrderNumber());

        // Публикуем событие о создании заказа
        orderEventPublisher.publishOrderCreated(savedOrder);

        return convertToDto(savedOrder);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderDto updateOrderStatus(Long orderId, UpdateOrderStatusDto updateDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Проверяем, что заказ не отменен
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderStatusException("Cannot update status of cancelled order");
        }

        // Обновляем статус заказа
        order.setStatus(updateDto.getStatus());
        order.setPaymentStatus(updateDto.getPaymentStatus());
        order.setTrackingNumber(updateDto.getTrackingNumber());

        // Если заказ подтвержден, обновляем количество товаров
        if (updateDto.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product.getStockQuantity() < item.getQuantity()) {
                    throw new OrderStatusException("Insufficient stock for product: " + product.getName());
                }

                UpdateProductDto productUpdate = new UpdateProductDto();
                productUpdate.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productService.updateProduct(product.getId(), productUpdate);
            }
        }

        Order savedOrder = orderRepository.save(order);
        return convertToDto(savedOrder);
    }

    @Transactional
    @CacheEvict(value = {"orders", "userOrders"}, allEntries = true)
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Проверяем, что заказ находится в статусе PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStatusException("Only pending orders can be cancelled");
        }

        // Отменяем заказ
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Освобождаем резервирование товаров
        for (OrderItem item : order.getItems()) {
            productReservationService.releaseReservationsForOrder(item.getProduct(), order.getUser());
        }
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