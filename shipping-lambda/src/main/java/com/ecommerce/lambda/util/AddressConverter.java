package com.ecommerce.lambda.util;

import com.ecommerce.lambda.dto.ShippoAddressDto;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;

/**
 * Утилитный класс для конвертации данных адреса
 */
public class AddressConverter {
    
    /**
     * Конвертирует OrderReadyForDeliveryEvent в ShippoAddressDto
     * @param event событие готовности заказа к доставке
     * @return адрес в формате Shippo API
     */
    public static ShippoAddressDto convertToShippoAddress(OrderReadyForDeliveryEvent event) {
        if (event == null) {
            return null;
        }
        
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
    
    /**
     * Создает текстовое представление адреса для email-уведомлений
     * @param event событие готовности заказа к доставке
     * @return строковое представление адреса
     */
    public static String formatAddressForEmail(OrderReadyForDeliveryEvent event) {
        if (event == null) {
            return "";
        }
        
        StringBuilder address = new StringBuilder();
        if (event.getShippingAddress() != null) {
            address.append(event.getShippingAddress());
        }
        if (event.getShippingCity() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(event.getShippingCity());
        }
        if (event.getShippingState() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(event.getShippingState());
        }
        if (event.getShippingZip() != null) {
            if (address.length() > 0) address.append(" ");
            address.append(event.getShippingZip());
        }
        if (event.getShippingCountry() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(event.getShippingCountry());
        }
        
        return address.toString();
    }
} 