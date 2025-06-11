package com.ecommerce.products.controller;

import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.products.dto.CreateReservationRequest;
import com.ecommerce.products.dto.ProductReservationDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductReservation;
import com.ecommerce.products.service.ProductReservationService;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class ProductReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductReservationService productReservationService;

    @MockBean
    private UserService userService;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    private ProductReservationDto testReservationDto;
    private ProductReservation testReservation;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setStockQuantity(10);

        testReservation = new ProductReservation();
        testReservation.setId(1L);
        testReservation.setProduct(testProduct);
        testReservation.setUser(testUser);
        testReservation.setQuantity(2);
        testReservation.setReservedAt(LocalDateTime.now());
        testReservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        testReservation.setActive(true);

        testReservationDto = new ProductReservationDto();
        testReservationDto.setId(1L);
        testReservationDto.setProductId(1L);
        testReservationDto.setUserId(1L);
        testReservationDto.setQuantity(2);
        testReservationDto.setReservedAt(testReservation.getReservedAt());
        testReservationDto.setExpiresAt(testReservation.getExpiresAt());
        testReservationDto.setActive(true);
    }

    @Test
    @WithMockUser
    void createReservation_ShouldReturnCreatedReservation() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(productReservationService.createReservation(any(), any())).thenReturn(testReservationDto);

        mockMvc.perform(post("/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.active").value(true));

        verify(productReservationService).createReservation(1L, 2);
    }

    @Test
    @WithMockUser
    void releaseReservation_ShouldReturnOk_WhenUserIsOwner() throws Exception {
        when(userService.getUserByEmail(any())).thenReturn(testUser);
        when(productReservationService.getReservationById(1L)).thenReturn(testReservation);

        mockMvc.perform(delete("/reservations/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(productReservationService).releaseReservation(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void releaseReservation_ShouldReturnOk_WhenUserIsAdmin() throws Exception {
        when(userService.getUserByEmail(any())).thenReturn(testUser);
        when(productReservationService.getReservationById(1L)).thenReturn(testReservation);

        mockMvc.perform(delete("/reservations/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(productReservationService).releaseReservation(1L);
    }

    @Test
    @WithMockUser
    void releaseReservation_ShouldReturnForbidden_WhenUserIsNotOwner() throws Exception {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        testReservation.setUser(otherUser);

        when(userService.getUserByEmail(any())).thenReturn(testUser);
        when(productReservationService.getReservationById(1L)).thenReturn(testReservation);

        mockMvc.perform(delete("/reservations/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(productReservationService, never()).releaseReservation(any());
    }

    @Test
    @WithMockUser
    void getUserReservations_ShouldReturnUserReservations() throws Exception {
        List<ProductReservationDto> reservations = Arrays.asList(testReservationDto);
        when(productReservationService.getUserReservations()).thenReturn(reservations);

        mockMvc.perform(get("/reservations/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].quantity").value(2));

        verify(productReservationService).getUserReservations();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProductReservations_ShouldReturnProductReservations() throws Exception {
        List<ProductReservationDto> reservations = Arrays.asList(testReservationDto);
        when(productReservationService.getProductReservationsDto(1L)).thenReturn(reservations);

        mockMvc.perform(get("/reservations/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].quantity").value(2));

        verify(productReservationService).getProductReservationsDto(1L);
    }

    //@Test
    //@WithMockUser(roles = "USER")
    //void getProductReservations_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
    //    mockMvc.perform(get("/reservations/product/1"))
    //            .andExpect(status().isForbidden());

    //    verify(productReservationService, never()).getProductReservationsDto(any());
    //}
} 