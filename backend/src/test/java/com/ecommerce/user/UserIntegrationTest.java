package com.ecommerce.user;

import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    protected User testUser;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        userRepository.deleteAll();

        // Создаем тестового пользователя
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCognitoId("test-cognito-id");
        testUser = userRepository.save(testUser);
    }
} 