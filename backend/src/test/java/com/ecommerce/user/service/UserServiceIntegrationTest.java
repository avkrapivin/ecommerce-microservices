package com.ecommerce.user.service;

import com.ecommerce.user.UserIntegrationTest;
import com.ecommerce.user.dto.UserProfileDto;
import com.ecommerce.user.entity.User;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.event.OrderEventPublisher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class UserServiceIntegrationTest extends UserIntegrationTest {

    @Autowired
    private UserService userService;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @Test
    void getUserById_ShouldReturnUser() {
        User user = userService.getUserById(testUser.getId());
        assertNotNull(user);
        assertEquals(testUser.getEmail(), user.getEmail());
    }

    @Test
    void getUserProfile_ShouldReturnUserProfile() {
        UserProfileDto profile = userService.getUserProfile(testUser.getEmail());
        assertNotNull(profile);
        assertEquals(testUser.getFirstName(), profile.getFirstName());
    }

    @Test
    void updateUserProfile_ShouldUpdateProfile() {
        UserProfileDto update = new UserProfileDto();
        update.setFirstName("Updated");
        update.setLastName("Name");

        userService.updateUserProfile(testUser.getEmail(), update);
        UserProfileDto result = userService.getUserProfile(testUser.getEmail());

        assertEquals("Updated", result.getFirstName());
        assertEquals("Name", result.getLastName());
    }

    @Test
    void getUserByEmail_ShouldThrowException_WhenUserNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("no@user.com"));
    }
}
