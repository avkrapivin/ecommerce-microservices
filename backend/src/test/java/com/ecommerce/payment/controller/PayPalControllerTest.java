// package com.ecommerce.payment.controller;

// import com.ecommerce.order.event.OrderEventPublisher;
// import com.ecommerce.payment.config.TestPayPalConfig;
// import com.ecommerce.payment.dto.PayPalPaymentResponse;
// import com.ecommerce.payment.service.PayPalService;
// import com.paypal.api.payments.Payment;
// import com.paypal.api.payments.Transaction;
// import com.paypal.base.rest.PayPalRESTException;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
// import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
// import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.context.annotation.Import;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;

// import java.util.ArrayList;
// import java.util.List;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest
// @AutoConfigureMockMvc
// @ActiveProfiles("test")
// @EnableAutoConfiguration(exclude = {
//     org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
//     org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
// })
// @WithMockUser
// public class PayPalControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @MockBean
//     private PayPalService payPalService;

//     @MockBean
//     private OrderEventPublisher orderEventPublisher;

//     private Payment mockPayment;

//     @BeforeEach
//     void setUp() throws PayPalRESTException {
//         mockPayment = new Payment();
//         mockPayment.setId("PAY-123");
//         mockPayment.setState("created");
        
//         Transaction transaction = new Transaction();
//         com.paypal.api.payments.Amount amount = new com.paypal.api.payments.Amount();
//         amount.setTotal("10.00");
//         amount.setCurrency("USD");
//         transaction.setAmount(amount);
//         transaction.setDescription("Test Payment");
        
//         List<Transaction> transactions = new ArrayList<>();
//         transactions.add(transaction);
//         mockPayment.setTransactions(transactions);

//         com.paypal.api.payments.Links approvalLink = new com.paypal.api.payments.Links();
//         approvalLink.setRel("approval_url");
//         approvalLink.setHref("https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=EC-123");
        
//         List<com.paypal.api.payments.Links> links = new ArrayList<>();
//         links.add(approvalLink);
//         mockPayment.setLinks(links);
//     }

//     @Test
//     void createPayment_Success() throws Exception {
//         when(payPalService.createPayment(any(), any(), any(), any(), any(), any(), any()))
//                 .thenReturn(mockPayment);

//         mockMvc.perform(post("/payments/paypal/create")
//                 .param("amount", "10.00")
//                 .param("currency", "USD")
//                 .param("description", "Test Payment"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.paymentId").value("PAY-123"))
//                 .andExpect(jsonPath("$.status").value("created"))
//                 .andExpect(jsonPath("$.redirectUrl").value("https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=EC-123"))
//                 .andExpect(jsonPath("$.paymentDetails.amount").value("10.00"))
//                 .andExpect(jsonPath("$.paymentDetails.currency").value("USD"))
//                 .andExpect(jsonPath("$.paymentDetails.description").value("Test Payment"));
//     }

//     @Test
//     void executePayment_Success() throws Exception {
//         Payment approvedPayment = new Payment();
//         approvedPayment.setId("PAY-123");
//         approvedPayment.setState("approved");
        
//         when(payPalService.executePayment(anyString(), anyString()))
//                 .thenReturn(approvedPayment);

//         mockMvc.perform(get("/payments/paypal/execute")
//                 .param("paymentId", "PAY-123")
//                 .param("PayerID", "PAYER-123"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.paymentId").value("PAY-123"))
//                 .andExpect(jsonPath("$.status").value("approved"));
//     }

//     @Test
//     void executePayment_Failed() throws Exception {
//         Payment failedPayment = new Payment();
//         failedPayment.setId("PAY-123");
//         failedPayment.setState("failed");
        
//         when(payPalService.executePayment(anyString(), anyString()))
//                 .thenReturn(failedPayment);

//         mockMvc.perform(get("/payments/paypal/execute")
//                 .param("paymentId", "PAY-123")
//                 .param("PayerID", "PAYER-123"))
//                 .andExpect(status().isBadRequest())
//                 .andExpect(jsonPath("$.status").value("ERROR"))
//                 .andExpect(jsonPath("$.errorMessage").value("Payment status: failed"));
//     }

//     @Test
//     void cancelPayment_Success() throws Exception {
//         mockMvc.perform(get("/payments/paypal/cancel"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.status").value("CANCELLED"))
//                 .andExpect(jsonPath("$.errorMessage").value("Payment was cancelled by user"));
//     }
// } 