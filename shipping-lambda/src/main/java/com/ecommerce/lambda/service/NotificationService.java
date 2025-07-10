package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для обработки уведомлений
 */
@Slf4j
public class NotificationService {
    
    private final EmailService emailService;
    
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    /**
     * Обрабатывает событие успешной оплаты
     */
    public void processPaymentCompleted(PaymentCompletedEvent event) {
        try {
            if (event == null) {
                throw new IllegalArgumentException("PaymentCompletedEvent cannot be null");
            }
            
            log.info("Processing payment completed notification for order: {}", event.getOrderNumber());
            
            validatePaymentCompletedEvent(event);
            emailService.sendPaymentCompletedEmail(event);
            
            log.info("Successfully processed payment completed notification for order: {}", 
                event.getOrderNumber());
            
        } catch (Exception e) {
            String orderNumber = event != null ? event.getOrderNumber() : "unknown";
            log.error("Failed to process payment completed notification for order {}: {}", 
                orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to process payment completed notification", e);
        }
    }
    
    /**
     * Обрабатывает событие готовности заказа к доставке
     */
    public void processOrderReadyForDelivery(OrderReadyForDeliveryEvent event) {
        try {
            if (event == null) {
                throw new IllegalArgumentException("OrderReadyForDeliveryEvent cannot be null");
            }
            
            log.info("Processing order ready for delivery notification for order: {}", 
                event.getOrderNumber());
            
            validateOrderReadyForDeliveryEvent(event);
            emailService.sendOrderReadyForDeliveryEmail(event);
            
            log.info("Successfully processed order ready for delivery notification for order: {}", 
                event.getOrderNumber());
            
        } catch (Exception e) {
            String orderNumber = event != null ? event.getOrderNumber() : "unknown";
            log.error("Failed to process order ready for delivery notification for order {}: {}", 
                orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to process order ready for delivery notification", e);
        }
    }
    
    /**
     * Обрабатывает событие обновления статуса заказа
     */
    public void processOrderStatusUpdated(OrderStatusUpdateEvent event) {
        try {
            if (event == null) {
                throw new IllegalArgumentException("OrderStatusUpdateEvent cannot be null");
            }
            
            log.info("Processing order status updated notification for order: {} to status: {}", 
                event.getOrderNumber(), event.getStatus());
            
            validateOrderStatusUpdateEvent(event);
            emailService.sendOrderStatusUpdatedEmail(event);
            
            log.info("Successfully processed order status updated notification for order: {}", 
                event.getOrderNumber());
            
        } catch (Exception e) {
            String orderNumber = event != null ? event.getOrderNumber() : "unknown";
            log.error("Failed to process order status updated notification for order {}: {}", 
                orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to process order status updated notification", e);
        }
    }
    
    /**
     * Обрабатывает событие успешного создания доставки
     */
    public void processShippingInitiated(OrderStatusUpdateEvent event) {
        try {
            log.info("Processing shipping initiated notification for order: {}", 
                event.getOrderNumber());
            
            validateOrderStatusUpdateEvent(event);
            
            // Создаем событие доставки из статуса заказа
            OrderReadyForDeliveryEvent deliveryEvent = convertToDeliveryEvent(event);
            emailService.sendOrderReadyForDeliveryEmail(deliveryEvent);
            
            log.info("Successfully processed shipping initiated notification for order: {}", 
                event.getOrderNumber());
            
        } catch (Exception e) {
            String orderNumber = event != null ? event.getOrderNumber() : "unknown";
            log.error("Failed to process shipping initiated notification for order {}: {}", 
                orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to process shipping initiated notification", e);
        }
    }
    
    /**
     * Обрабатывает событие ошибки создания доставки
     */
    public void processShippingError(OrderStatusUpdateEvent event) {
        try {
            log.info("Processing shipping error notification for order: {}", 
                event.getOrderNumber());
            
            validateOrderStatusUpdateEvent(event);
            
            // Отправляем email об ошибке доставки
            emailService.sendOrderStatusUpdatedEmail(event);
            
            log.info("Successfully processed shipping error notification for order: {}", 
                event.getOrderNumber());
            
        } catch (Exception e) {
            String orderNumber = event != null ? event.getOrderNumber() : "unknown";
            log.error("Failed to process shipping error notification for order {}: {}", 
                orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to process shipping error notification", e);
        }
    }
    
    private void validatePaymentCompletedEvent(PaymentCompletedEvent event) {
        // event уже проверен на null в вызывающем методе
        if (event.getOrderNumber() == null || event.getOrderNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        if (event.getCustomerEmail() == null || event.getCustomerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        if (event.getCustomerName() == null || event.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (!isValidEmail(event.getCustomerEmail())) {
            throw new IllegalArgumentException("Invalid customer email format: " + event.getCustomerEmail());
        }
    }
    
    private void validateOrderReadyForDeliveryEvent(OrderReadyForDeliveryEvent event) {
        // event уже проверен на null в вызывающем методе
        if (event.getOrderNumber() == null || event.getOrderNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        if (event.getCustomerEmail() == null || event.getCustomerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        if (event.getCustomerName() == null || event.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (event.getShippingAddress() == null) {
            throw new IllegalArgumentException("Shipping address cannot be null");
        }
        if (!isValidEmail(event.getCustomerEmail())) {
            throw new IllegalArgumentException("Invalid customer email format: " + event.getCustomerEmail());
        }
    }
    
    private void validateOrderStatusUpdateEvent(OrderStatusUpdateEvent event) {
        // event уже проверен на null в вызывающем методе
        if (event.getOrderNumber() == null || event.getOrderNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        if (event.getCustomerEmail() == null || event.getCustomerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        if (event.getStatus() == null || event.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        if (!isValidEmail(event.getCustomerEmail())) {
            throw new IllegalArgumentException("Invalid customer email format: " + event.getCustomerEmail());
        }
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Простая валидация email
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Конвертирует OrderStatusUpdateEvent в OrderReadyForDeliveryEvent для email уведомлений
     */
    private OrderReadyForDeliveryEvent convertToDeliveryEvent(OrderStatusUpdateEvent statusEvent) {
        OrderReadyForDeliveryEvent deliveryEvent = new OrderReadyForDeliveryEvent();
        deliveryEvent.setOrderId(statusEvent.getOrderId());
        deliveryEvent.setOrderNumber(statusEvent.getOrderNumber());
        deliveryEvent.setCustomerEmail(statusEvent.getCustomerEmail());
        deliveryEvent.setCustomerName(statusEvent.getCustomerName());
        
        // Заполняем базовые поля (адрес может быть неполным, но это не критично для email)
        // В реальной системе можно получить полные данные заказа из базы данных
        deliveryEvent.setShippingAddress("Shipping address");
        deliveryEvent.setShippingCity("City");
        deliveryEvent.setShippingState("State");
        deliveryEvent.setShippingZip("ZIP");
        deliveryEvent.setShippingCountry("Country");
        
        return deliveryEvent;
    }
} 