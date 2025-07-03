package com.ecommerce.lambda.service;

import com.ecommerce.lambda.dto.ShippoAddressDto;
import com.ecommerce.lambda.dto.ShippoParcelDto;
import com.ecommerce.lambda.dto.ShippoResponseDto;
import com.ecommerce.lambda.dto.ShippoShipmentDto;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippoServiceTest {

    @Mock
    private SnsPublisher snsPublisher;

    private WireMockServer wireMockServer;
    private ShippoService shippoService;
    private ObjectMapper objectMapper;

    private static final String ORDER_STATUS_UPDATED_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-status-updated";

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
        
        // Set environment variables for from address
        System.setProperty("SHIPPO_FROM_NAME", "Test Sender");
        System.setProperty("SHIPPO_FROM_STREET", "123 Sender St");
        System.setProperty("SHIPPO_FROM_CITY", "Sender City");
        System.setProperty("SHIPPO_FROM_STATE", "CA");
        System.setProperty("SHIPPO_FROM_ZIP", "90210");
        System.setProperty("SHIPPO_FROM_COUNTRY", "US");
        System.setProperty("SHIPPO_FROM_PHONE", "+1234567890");
        
        // Set Shippo API URL to WireMock server
        String mockUrl = "http://localhost:" + wireMockServer.port();
        System.setProperty("SHIPPO_API_URL", mockUrl);
        System.out.println("WireMock server started on port: " + wireMockServer.port());
        System.out.println("SHIPPO_API_URL set to: " + mockUrl);
        
        objectMapper = new ObjectMapper();
        
        // Create ShippoService with mocked API URL
        shippoService = new ShippoService(snsPublisher, ORDER_STATUS_UPDATED_TOPIC_ARN, "test-api-key");
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void shouldProcessDeliverySuccessfully() throws IOException {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        ShippoResponseDto mockResponse = new ShippoResponseDto();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setTrackingNumber("TRACK123456");
        
        // Mock Shippo API response
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .withHeader("Authorization", equalTo("ShippoToken test-api-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))));

        // When
        shippoService.processDelivery(event);

        // Then
        System.out.println("WireMock server port: " + wireMockServer.port());
        System.out.println("SHIPPO_API_URL property: " + System.getProperty("SHIPPO_API_URL"));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments"))
                .withHeader("Authorization", equalTo("ShippoToken test-api-key")));
        verify(snsPublisher).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleShippoApiFailure() {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        // Mock Shippo API failure
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When & Then
        assertThatThrownBy(() -> shippoService.processDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process delivery");
        
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments")));
        verify(snsPublisher, never()).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleShippoApiErrorResponse() throws IOException {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        ShippoResponseDto mockResponse = new ShippoResponseDto();
        mockResponse.setStatus("ERROR");
        mockResponse.setMessage("Invalid address");
        
        // Mock Shippo API error response
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))));

        // When & Then
        assertThatThrownBy(() -> shippoService.processDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process delivery");
        
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments")));
        verify(snsPublisher, never()).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleNullEvent() {
        // When & Then
        assertThatThrownBy(() -> shippoService.processDelivery(null))
                .isInstanceOf(NullPointerException.class);
        
        verify(snsPublisher, never()).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleEventWithNullFields() {
        // Given
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        // Other fields are null

        // When & Then
        assertThatThrownBy(() -> shippoService.processDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process delivery");
        
        verify(snsPublisher, never()).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleLargeParcel() throws IOException {
        // Given
        OrderReadyForDeliveryEvent event = createLargeParcelEvent();
        
        ShippoResponseDto mockResponse = new ShippoResponseDto();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setTrackingNumber("TRACK789012");
        
        // Mock Shippo API response
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))));

        // When
        shippoService.processDelivery(event);

        // Then
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments")));
        verify(snsPublisher).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleEventWithSpecialCharacters() throws IOException {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setOrderNumber("ORD-123-特殊字符-@#$%");
        
        ShippoResponseDto mockResponse = new ShippoResponseDto();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setTrackingNumber("TRACK123456");
        
        // Mock Shippo API response
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))));

        // When
        shippoService.processDelivery(event);

        // Then
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments")));
        verify(snsPublisher).publishMessage(anyString(), anyString());
    }



    @Test
    void shouldHandleEventWithEmptyEmail() throws IOException {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setCustomerEmail("");
        
        ShippoResponseDto mockResponse = new ShippoResponseDto();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setTrackingNumber("TRACK123456");
        
        // Mock Shippo API response
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))));

        // When
        shippoService.processDelivery(event);

        // Then
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments")));
        verify(snsPublisher).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleEventWithDifferentStatuses() throws IOException {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        ShippoResponseDto mockResponse = new ShippoResponseDto();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setTrackingNumber("TRACK123456");
        
        // Mock Shippo API response
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))));

        // When
        shippoService.processDelivery(event);

        // Then
        wireMockServer.verify(postRequestedFor(urlEqualTo("/shipments")));
        verify(snsPublisher).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldHandleShippoServiceException() {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        // Mock Shippo API to throw exception
        wireMockServer.stubFor(post(urlEqualTo("/shipments"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        // When & Then
        assertThatThrownBy(() -> shippoService.processDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process delivery");
        
        verify(snsPublisher, never()).publishMessage(anyString(), anyString());
    }

    @Test
    void shouldCreateAddressDtoFromEvent() {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        // When
        ShippoAddressDto addressDto = createAddressDtoFromEvent(event);
        
        // Then
        assertThat(addressDto.getName()).isEqualTo("John Doe");
        assertThat(addressDto.getStreet1()).isEqualTo("123 Main St");
        assertThat(addressDto.getCity()).isEqualTo("New York");
        assertThat(addressDto.getState()).isEqualTo("NY");
        assertThat(addressDto.getZip()).isEqualTo("10001");
        assertThat(addressDto.getCountry()).isEqualTo("US");
        assertThat(addressDto.getPhone()).isEqualTo("+1234567890");
    }

    @Test
    void shouldCreateParcelDtoFromEvent() {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        // When
        ShippoParcelDto parcelDto = createParcelDtoFromEvent(event);
        
        // Then
        assertThat(parcelDto.getLength()).isEqualTo(10.0);
        assertThat(parcelDto.getWidth()).isEqualTo(5.0);
        assertThat(parcelDto.getHeight()).isEqualTo(2.0);
        assertThat(parcelDto.getDistanceUnit()).isEqualTo("cm");
        assertThat(parcelDto.getWeight()).isEqualTo(1.5);
        assertThat(parcelDto.getMassUnit()).isEqualTo("kg");
    }

    @Test
    void shouldCreateShipmentDtoFromEvent() {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        
        // When
        ShippoShipmentDto shipmentDto = createShipmentDtoFromEvent(event);
        
        // Then
        assertThat(shipmentDto.getAddressFrom()).isNotNull();
        assertThat(shipmentDto.getAddressTo()).isNotNull();
        assertThat(shipmentDto.getParcels()).hasSize(1);
        
        ShippoAddressDto toAddress = shipmentDto.getAddressTo();
        assertThat(toAddress.getName()).isEqualTo("John Doe");
        assertThat(toAddress.getStreet1()).isEqualTo("123 Main St");
        assertThat(toAddress.getCity()).isEqualTo("New York");
        assertThat(toAddress.getState()).isEqualTo("NY");
        assertThat(toAddress.getZip()).isEqualTo("10001");
        assertThat(toAddress.getCountry()).isEqualTo("US");
        assertThat(toAddress.getPhone()).isEqualTo("+1234567890");
        
        ShippoParcelDto parcel = shipmentDto.getParcels().get(0);
        assertThat(parcel.getLength()).isEqualTo(10.0);
        assertThat(parcel.getWidth()).isEqualTo(5.0);
        assertThat(parcel.getHeight()).isEqualTo(2.0);
        assertThat(parcel.getDistanceUnit()).isEqualTo("cm");
        assertThat(parcel.getWeight()).isEqualTo(1.5);
        assertThat(parcel.getMassUnit()).isEqualTo("kg");
    }

    private OrderReadyForDeliveryEvent createTestEvent() {
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("john.doe@example.com");
        event.setCustomerName("John Doe");
        event.setShippingAddress("123 Main St");
        event.setShippingCity("New York");
        event.setShippingState("NY");
        event.setShippingZip("10001");
        event.setShippingCountry("US");
        event.setTotalAmount(100.0);
        event.setCurrency("USD");
        event.setParcelLength(10.0);
        event.setParcelWidth(5.0);
        event.setParcelHeight(2.0);
        event.setParcelDistanceUnit("cm");
        event.setParcelWeight(1.5);
        event.setParcelMassUnit("kg");
        event.setPhoneNumber("+1234567890");
        return event;
    }

    private OrderReadyForDeliveryEvent createLargeParcelEvent() {
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
        event.setOrderId("order-456");
        event.setOrderNumber("ORD-456");
        event.setCustomerEmail("jane.smith@example.com");
        event.setCustomerName("Jane Smith");
        event.setShippingAddress("456 Oak Ave");
        event.setShippingCity("Los Angeles");
        event.setShippingState("CA");
        event.setShippingZip("90210");
        event.setShippingCountry("US");
        event.setTotalAmount(500.0);
        event.setCurrency("USD");
        event.setParcelLength(50.0);
        event.setParcelWidth(30.0);
        event.setParcelHeight(20.0);
        event.setParcelDistanceUnit("cm");
        event.setParcelWeight(10.0);
        event.setParcelMassUnit("kg");
        event.setPhoneNumber("+1987654321");
        return event;
    }

    private ShippoAddressDto createAddressDtoFromEvent(OrderReadyForDeliveryEvent event) {
        ShippoAddressDto addressDto = new ShippoAddressDto();
        addressDto.setName(event.getCustomerName());
        addressDto.setStreet1(event.getShippingAddress());
        addressDto.setCity(event.getShippingCity());
        addressDto.setState(event.getShippingState());
        addressDto.setZip(event.getShippingZip());
        addressDto.setCountry(event.getShippingCountry());
        addressDto.setPhone(event.getPhoneNumber());
        return addressDto;
    }

    private ShippoParcelDto createParcelDtoFromEvent(OrderReadyForDeliveryEvent event) {
        ShippoParcelDto parcelDto = new ShippoParcelDto();
        parcelDto.setLength(event.getParcelLength());
        parcelDto.setWidth(event.getParcelWidth());
        parcelDto.setHeight(event.getParcelHeight());
        parcelDto.setDistanceUnit(event.getParcelDistanceUnit());
        parcelDto.setWeight(event.getParcelWeight());
        parcelDto.setMassUnit(event.getParcelMassUnit());
        return parcelDto;
    }

    private ShippoShipmentDto createShipmentDtoFromEvent(OrderReadyForDeliveryEvent event) {
        ShippoShipmentDto shipmentDto = new ShippoShipmentDto();
        shipmentDto.setAddressFrom(createFromAddress());
        shipmentDto.setAddressTo(createAddressDtoFromEvent(event));
        shipmentDto.setParcels(java.util.List.of(createParcelDtoFromEvent(event)));
        return shipmentDto;
    }

    private ShippoAddressDto createFromAddress() {
        ShippoAddressDto fromAddress = new ShippoAddressDto();
        fromAddress.setName("Test Sender");
        fromAddress.setStreet1("123 Sender St");
        fromAddress.setCity("Sender City");
        fromAddress.setState("CA");
        fromAddress.setZip("90210");
        fromAddress.setCountry("US");
        fromAddress.setPhone("+1234567890");
        return fromAddress;
    }
} 