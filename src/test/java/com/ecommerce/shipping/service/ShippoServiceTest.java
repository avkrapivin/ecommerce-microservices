package com.ecommerce.shipping.service;

import com.ecommerce.shipping.config.ShippoProperties;
import com.ecommerce.shipping.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippoServiceTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    @Mock
    private Response response;

    @Mock
    private ResponseBody responseBody;

    @Mock
    private ShippoProperties shippoProperties;

    @Mock
    private ShippoProperties.Api api;

    @InjectMocks
    private ShippoService shippoService;

    private ObjectMapper objectMapper;
    private ShippoAddressDto addressDto;
    private ShippoParcelDto parcelDto;
    private ShippoShipmentDto shipmentDto;
    private ShippingRateRequestDto rateRequest;
    private ShippingLabelRequestDto labelRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        shippoService = new ShippoService(objectMapper, httpClient, shippoProperties);
        when(shippoProperties.getApi()).thenReturn(api);
        when(api.getKey()).thenReturn("test_key");

        // Настройка тестовых данных
        addressDto = new ShippoAddressDto();
        addressDto.setName("John Doe");
        addressDto.setStreet1("123 Main St");
        addressDto.setCity("New York");
        addressDto.setState("NY");
        addressDto.setZip("10001");
        addressDto.setCountry("US");

        parcelDto = new ShippoParcelDto();
        parcelDto.setLength(5.0);
        parcelDto.setWidth(5.0);
        parcelDto.setHeight(5.0);
        parcelDto.setDistanceUnit("in");
        parcelDto.setWeight(2.0);
        parcelDto.setMassUnit("lb");

        shipmentDto = new ShippoShipmentDto();
        shipmentDto.setAddressFrom(addressDto);
        shipmentDto.setAddressTo(addressDto);
        shipmentDto.setParcels(List.of(parcelDto));

        rateRequest = new ShippingRateRequestDto();
        rateRequest.setAddressFrom(addressDto);
        rateRequest.setAddressTo(addressDto);
        rateRequest.setParcels(List.of(parcelDto));

        labelRequest = new ShippingLabelRequestDto();
        labelRequest.setRateId("rate123");
        labelRequest.setAsync(false);
        labelRequest.setLabelFormat("PDF");
        labelRequest.setLabelSize("4x6");
    }

    @Test
    void createAddress_WhenSuccessful_ShouldReturnResponse() throws IOException {
        // Подготовка
        String successResponse = "{\"objectId\":\"addr123\",\"status\":\"SUCCESS\"}";
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(successResponse);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Выполнение
        ShippoResponseDto result = shippoService.createAddress(addressDto);

        // Проверка
        assertNotNull(result);
        assertEquals("addr123", result.getObjectId());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void createAddress_WhenFailed_ShouldThrowException() throws IOException {
        // Подготовка
        when(response.isSuccessful()).thenReturn(false);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("Error");
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Проверка
        assertThrows(IOException.class, () -> shippoService.createAddress(addressDto));
    }

    @Test
    void calculateShippingRates_WhenSuccessful_ShouldReturnRates() throws IOException {
        // Подготовка
        String shipmentResponse = "{\"objectId\":\"ship123\",\"status\":\"SUCCESS\"}";
        String ratesResponse = "[{\"objectId\":\"rate123\",\"provider\":\"USPS\",\"service\":\"Priority\",\"amount\":\"10.00\"}]";
        
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string())
            .thenReturn(shipmentResponse)
            .thenReturn(ratesResponse);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Выполнение
        ShippingRateResponseDto result = shippoService.calculateShippingRates(rateRequest);

        // Проверка
        assertNotNull(result);
        assertNotNull(result.getRates());
        assertFalse(result.getRates().isEmpty());
        assertEquals("rate123", result.getRates().get(0).getObjectId());
    }

    @Test
    void generateShippingLabel_WhenSuccessful_ShouldReturnLabel() throws IOException {
        // Подготовка
        String successResponse = "{\"objectId\":\"trans123\",\"status\":\"SUCCESS\",\"label_url\":\"https://label.com/label.pdf\",\"tracking_number\":\"TRACK123\",\"tracking_url_provider\":\"https://tracking.com/TRACK123\"}";
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(successResponse);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Выполнение
        ShippingLabelResponseDto result = shippoService.generateShippingLabel(labelRequest);

        // Проверка
        assertNotNull(result);
        assertEquals("trans123", result.getObjectId());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("https://label.com/label.pdf", result.getLabelUrl());
        assertEquals("TRACK123", result.getTrackingNumber());
        assertEquals("https://tracking.com/TRACK123", result.getTrackingUrlProvider());
    }

    @Test
    void generateShippingLabel_WhenFailed_ShouldThrowException() throws IOException {
        // Подготовка
        when(response.isSuccessful()).thenReturn(false);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("Error");
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Проверка
        assertThrows(IOException.class, () -> shippoService.generateShippingLabel(labelRequest));
    }
} 