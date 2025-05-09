package com.ecommerce.shipping.service;

import com.ecommerce.shipping.config.ShippoProperties;
import com.ecommerce.shipping.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippoService {
    private static final String SHIPPO_API_URL = "https://api.goshippo.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final ShippoProperties shippoProperties;

    public ShippoResponseDto createAddress(ShippoAddressDto addressDto) throws IOException {
        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(addressDto), JSON);
        
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/addresses")
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to create address: {}", response.body().string());
                throw new IOException("Failed to create address: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ShippoResponseDto.class);
        }
    }

    public ShippoResponseDto createParcel(ShippoParcelDto parcelDto) throws IOException {
        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(parcelDto), JSON);
        
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/parcels")
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to create parcel: {}", response.body().string());
                throw new IOException("Failed to create parcel: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ShippoResponseDto.class);
        }
    }

    public ShippoResponseDto createShipment(ShippoShipmentDto shipmentDto) throws IOException {
        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(shipmentDto), JSON);
        
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/shipments")
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to create shipment: {}", response.body().string());
                throw new IOException("Failed to create shipment: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ShippoResponseDto.class);
        }
    }

    public List<ShippoRateDto> getShippingRates(String shipmentId) throws IOException {
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/shipments/" + shipmentId + "/rates")
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to get shipping rates: {}", response.body().string());
                throw new IOException("Failed to get shipping rates: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), new TypeReference<List<ShippoRateDto>>() {});
        }
    }

    public ShippoResponseDto createLabel(String rateId) throws IOException {
        RequestBody body = RequestBody.create(
                "{\"rate\":\"" + rateId + "\",\"async\":false}",
                JSON
        );
        
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/transactions")
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to create label: {}", response.body().string());
                throw new IOException("Failed to create label: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ShippoResponseDto.class);
        }
    }

    public ShippoTrackingDto getTrackingInfo(String trackingNumber) throws IOException {
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/tracks/" + trackingNumber)
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to get tracking info: {}", response.body().string());
                throw new IOException("Failed to get tracking info: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ShippoTrackingDto.class);
        }
    }

    public ShippoTrackingDto getTrackingInfoByCarrier(String trackingNumber, String carrier) throws IOException {
        Request request = new Request.Builder()
                .url(SHIPPO_API_URL + "/tracks/" + carrier + "/" + trackingNumber)
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to get tracking info: {}", response.body().string());
                throw new IOException("Failed to get tracking info: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), ShippoTrackingDto.class);
        }
    }

    public ShippingRateResponseDto calculateShippingRates(ShippingRateRequestDto request) throws IOException {
        // Создаем отправление для расчета
        ShippoShipmentDto shipmentDto = new ShippoShipmentDto();
        shipmentDto.setAddressFrom(request.getAddressFrom());
        shipmentDto.setAddressTo(request.getAddressTo());
        shipmentDto.setParcels(request.getParcels());

        // Создаем отправление в Shippo
        ShippoResponseDto shipmentResponse = createShipment(shipmentDto);
        if (shipmentResponse.getStatus() != null && !shipmentResponse.getStatus().equals("SUCCESS")) {
            ShippingRateResponseDto response = new ShippingRateResponseDto();
            response.setError(shipmentResponse.getMessage());
            return response;
        }

        // Получаем тарифы
        List<ShippoRateDto> rates = getShippingRates(shipmentResponse.getObjectId());
        
        // Преобразуем в наш формат ответа
        ShippingRateResponseDto response = new ShippingRateResponseDto();
        response.setRates(rates.stream()
                .map(rate -> {
                    ShippingRateResponseDto.ShippingRateDto dto = new ShippingRateResponseDto.ShippingRateDto();
                    dto.setObjectId(rate.getObjectId());
                    dto.setProvider(rate.getProvider());
                    dto.setService(rate.getService());
                    dto.setCurrency(rate.getCurrency());
                    dto.setAmount(rate.getAmount());
                    dto.setDays(rate.getDays());
                    dto.setEstimatedDays(rate.getEstimatedDays());
                    dto.setDurationTerms(rate.getDurationTerms());
                    dto.setProviderImage75(rate.getProviderImage75());
                    dto.setProviderImage200(rate.getProviderImage200());
                    return dto;
                })
                .toList());

        return response;
    }

    public ShippingLabelResponseDto generateShippingLabel(ShippingLabelRequestDto labelRequest) throws IOException {
        // Создаем тело запроса для генерации лейбла
        String requestBody = String.format(
                "{\"rate\":\"%s\",\"async\":%b,\"label_file_type\":\"%s\",\"label_size\":\"%s\"}",
                labelRequest.getRateId(),
                labelRequest.isAsync(),
                labelRequest.getLabelFormat(),
                labelRequest.getLabelSize()
        );

        RequestBody body = RequestBody.create(requestBody, JSON);
        
        Request httpRequest = new Request.Builder()
                .url(SHIPPO_API_URL + "/transactions")
                .addHeader("Authorization", "ShippoToken " + shippoProperties.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to generate shipping label: {}", response.body().string());
                throw new IOException("Failed to generate shipping label: " + response.code());
            }

            // Получаем ответ от Shippo
            ShippoResponseDto shippoResponse = objectMapper.readValue(response.body().string(), ShippoResponseDto.class);
            
            // Преобразуем в наш формат ответа
            ShippingLabelResponseDto responseDto = new ShippingLabelResponseDto();
            responseDto.setObjectId(shippoResponse.getObjectId());
            responseDto.setStatus(shippoResponse.getStatus());
            responseDto.setMessage(shippoResponse.getMessage());
            responseDto.setLabelUrl(shippoResponse.getLabelUrl());
            responseDto.setTrackingNumber(shippoResponse.getTrackingNumber());
            responseDto.setTrackingUrlProvider(shippoResponse.getTrackingUrlProvider());
            
            // Если есть ошибка, устанавливаем её
            if (shippoResponse.getStatus() != null && !shippoResponse.getStatus().equals("SUCCESS")) {
                responseDto.setError(shippoResponse.getMessage());
            }

            return responseDto;
        }
    }
} 