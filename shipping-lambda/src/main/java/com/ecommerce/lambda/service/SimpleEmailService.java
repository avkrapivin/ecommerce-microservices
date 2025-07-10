package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.util.AddressConverter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

/**
 * Простая реализация EmailService с текстовыми email через AWS SES
 */
@Slf4j
public class SimpleEmailService implements EmailService {
    
    private final SesV2Client sesClient;
    private final String fromEmail;
    
    public SimpleEmailService(SesV2Client sesClient, String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }
    
    @Override
    public void sendPaymentCompletedEmail(PaymentCompletedEvent event) {
        try {
            String subject = "Payment for Order #" + event.getOrderNumber() + " Successfully Completed";
            String body = String.format(
                "Hello %s!\n\n" +
                "Your order #%s has been successfully paid for the amount of %.2f %s.\n" +
                "Transaction ID: %s\n\n" +
                "Your order will be prepared for shipment shortly.\n" +
                "We will send you a notification as soon as your order is ready for delivery.\n\n" +
                "Thank you for your purchase!\n\n" +
                "Best regards,\n" +
                "E-commerce Team",
                event.getCustomerName(),
                event.getOrderNumber(),
                event.getTotalAmount(),
                event.getCurrency(),
                event.getPaymentId()
            );
            
            sendEmail(event.getCustomerEmail(), subject, body);
            log.info("Payment completed email sent to {} for order {}", 
                event.getCustomerEmail(), event.getOrderNumber());
            
        } catch (Exception e) {
            log.error("Failed to send payment completed email for order {}: {}", 
                event.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to send payment completed email", e);
        }
    }
    
    @Override
    public void sendOrderReadyForDeliveryEmail(OrderReadyForDeliveryEvent event) {
        try {
            String subject = "Order #" + event.getOrderNumber() + " Ready for Shipment";
            String formattedAddress = AddressConverter.formatAddressForEmail(event);
            String body = String.format(
                "Hello %s!\n\n" +
                "Great news! Your order #%s is ready for shipment.\n\n" +
                "Delivery details:\n" +
                "Address: %s\n\n" +
                "Your order will be handed over to the shipping service shortly.\n" +
                "A tracking number will be sent separately.\n\n" +
                "Best regards,\n" +
                "E-commerce Team",
                event.getCustomerName(),
                event.getOrderNumber(),
                formattedAddress
            );
            
            sendEmail(event.getCustomerEmail(), subject, body);
            log.info("Order ready for delivery email sent to {} for order {}", 
                event.getCustomerEmail(), event.getOrderNumber());
            
        } catch (Exception e) {
            log.error("Failed to send order ready email for order {}: {}", 
                event.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to send order ready email", e);
        }
    }
    
    @Override
    public void sendOrderStatusUpdatedEmail(OrderStatusUpdateEvent event) {
        try {
            String subject = "Order #" + event.getOrderNumber() + " Status Updated";
            String body = String.format(
                "Hello!\n\n" +
                "The status of your order #%s has been updated.\n\n" +
                "New status: %s\n" +
                "Updated at: %s\n\n" +
                "%s\n\n" +
                "Best regards,\n" +
                "E-commerce Team",
                event.getOrderNumber(),
                getStatusDisplayName(event.getStatus()),
                event.getUpdatedAt(),
                getStatusDescription(event.getStatus())
            );
            
            sendEmail(event.getCustomerEmail(), subject, body);
            log.info("Order status updated email sent to {} for order {}", 
                event.getCustomerEmail(), event.getOrderNumber());
            
        } catch (Exception e) {
            log.error("Failed to send order status updated email for order {}: {}", 
                event.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to send order status updated email", e);
        }
    }
    
    private void sendEmail(String toEmail, String subject, String body) {
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                .fromEmailAddress(fromEmail)
                .destination(Destination.builder()
                    .toAddresses(toEmail)
                    .build())
                .content(EmailContent.builder()
                    .simple(Message.builder()
                        .subject(Content.builder().data(subject).build())
                        .body(Body.builder()
                            .text(Content.builder().data(body).build())
                            .build())
                        .build())
                    .build())
                .build();
            
            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.debug("Email sent successfully. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private String getStatusDisplayName(String status) {
        return switch (status) {
            case "PAYMENT_COMPLETED" -> "Paid";
            case "READY_FOR_DELIVERY" -> "Ready for Shipment";
            case "IN_DELIVERY" -> "In Transit";
            case "DELIVERED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }
    
    private String getStatusDescription(String status) {
        return switch (status) {
            case "PAYMENT_COMPLETED" -> "Your order has been successfully paid and is being processed.";
            case "READY_FOR_DELIVERY" -> "Your order is ready for shipment and will be handed over to the shipping service soon.";
            case "IN_DELIVERY" -> "Your order is on its way. Track the status using the tracking number.";
            case "DELIVERED" -> "Your order has been successfully delivered. Thank you for your purchase!";
            case "CANCELLED" -> "Your order has been cancelled. If you did not initiate this cancellation, please contact customer support.";
            default -> "Your order status has been updated.";
        };
    }
} 