package com.ecommerce.lambda.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ecommerce.lambda.dto.*;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
public class ShippoService {
    private static final String SHIPPO_API_URL = "https://api.goshippo.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final SnsClient snsClient;
    private final String orderStatusUpdatedTopicArn;
    private final String shippoApiKey;
    private final ShippoAddressDto fromAddress;

    public ShippoService(SnsClient snsClient, String orderStatusUpdatedTopicArn, String shippoApiKey) {
        this.snsClient = snsClient;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.orderStatusUpdatedTopicArn = orderStatusUpdatedTopicArn;
        this.shippoApiKey = shippoApiKey;
        
        // Инициализация адреса отправителя из переменных окружения
        this.fromAddress = new ShippoAddressDto();
        this.fromAddress.setName(System.getenv("SHIPPO_FROM_NAME"));
        this.fromAddress.setStreet1(System.getenv("SHIPPO_FROM_STREET"));
        this.fromAddress.setCity(System.getenv("SHIPPO_FROM_CITY"));
        this.fromAddress.setState(System.getenv("SHIPPO_FROM_STATE"));
        this.fromAddress.setZip(System.getenv("SHIPPO_FROM_ZIP"));
        this.fromAddress.setCountry(System.getenv("SHIPPO_FROM_COUNTRY"));
        this.fromAddress.setPhone(System.getenv("SHIPPO_FROM_PHONE"));
    }

    public void processDelivery(OrderReadyForDeliveryEvent event) {
        try {
            log.info("Processing delivery for order {}", event.getOrderNumber());

            // Создаем адрес получателя
            ShippoAddressDto toAddress = new ShippoAddressDto();
            toAddress.setName(event.getCustomerName());
            toAddress.setStreet1(event.getShippingAddress());
            toAddress.setCity(event.getShippingCity());
            toAddress.setState(event.getShippingState());
            toAddress.setZip(event.getShippingZip());
            toAddress.setCountry(event.getShippingCountry());
            toAddress.setPhone(event.getPhoneNumber());

            // Создаем посылку из данных заказа
            ShippoParcelDto parcel = new ShippoParcelDto();
            parcel.setLength(event.getParcelLength());
            parcel.setWidth(event.getParcelWidth());
            parcel.setHeight(event.getParcelHeight());
            parcel.setDistanceUnit(event.getParcelDistanceUnit());
            parcel.setWeight(event.getParcelWeight());
            parcel.setMassUnit(event.getParcelMassUnit());

            // Создаем отправление
            ShippoShipmentDto shipmentDto = new ShippoShipmentDto();
            shipmentDto.setAddressFrom(fromAddress);
            shipmentDto.setAddressTo(toAddress);
            shipmentDto.setParcels(List.of(parcel));

            // Создаем отправление в Shippo с повторными попытками
            ShippoResponseDto shipmentResponse = createShipmentWithRetry(shipmentDto);
            if (shipmentResponse.getStatus() != null && !shipmentResponse.getStatus().equals("SUCCESS")) {
                throw new RuntimeException("Failed to create shipment: " + shipmentResponse.getMessage());
            }

            // Публикуем событие об обновлении статуса
            publishOrderStatusUpdate(event.getOrderId(), "SHIPPING_INITIATED", shipmentResponse.getTrackingNumber());
            
            log.info("Delivery processed successfully for order {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Error processing delivery for order {}: {}", event.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to process delivery", e);
        }
    }

    private ShippoResponseDto createShipmentWithRetry(ShippoShipmentDto shipmentDto) throws IOException {
        int retryCount = 0;
        int delayMs = INITIAL_RETRY_DELAY_MS;
        
        while (true) {
            try {
                return createShipment(shipmentDto);
            } catch (IOException e) {
                if (retryCount >= MAX_RETRIES) {
                    throw e;
                }
                
                // Проверяем, является ли ошибка временной (429 или 5xx)
                if (e.getMessage().contains("429") || e.getMessage().contains("5")) {
                    log.warn("Rate limit or server error, retrying in {} ms (attempt {}/{})", 
                            delayMs, retryCount + 1, MAX_RETRIES);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                    
                    // Экспоненциальная задержка
                    delayMs *= 2;
                    retryCount++;
                    continue;
                }
                
                // Если это не временная ошибка, пробрасываем её дальше
                throw e;
            }
        }
    }

    private ShippoResponseDto createShipment(ShippoShipmentDto shipmentDto) throws IOException {
        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(shipmentDto), JSON);
        
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/shipments")
                .addHeader("Authorization", "ShippoToken " + shippoApiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                log.error("Failed to create shipment: {}", responseBody);
                throw new IOException("Failed to create shipment: " + response.code());
            }
            return objectMapper.readValue(responseBody, ShippoResponseDto.class);
        }
    }

    private void publishOrderStatusUpdate(String orderId, String status, String trackingNumber) {
        try {
            String message = objectMapper.writeValueAsString(new OrderStatusUpdateEvent(orderId, status, trackingNumber));
            PublishRequest request = PublishRequest.builder()
                    .topicArn(orderStatusUpdatedTopicArn)
                    .message(message)
                    .subject("OrderStatusUpdated")
                    .build();

            snsClient.publish(request);
            log.info("Published order status update for order {}: {}", orderId, status);
        } catch (Exception e) {
            log.error("Error publishing order status update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish order status update", e);
        }
    }
} 