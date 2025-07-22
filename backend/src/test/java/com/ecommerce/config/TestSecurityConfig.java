package com.ecommerce.config;

import com.ecommerce.security.config.CognitoJwtGrantedAuthoritiesConverter;
import com.ecommerce.security.filter.JwtAuthenticationFilter;
import com.ecommerce.security.handler.CognitoLogoutHandler;
import com.ecommerce.security.util.JwtUtil;
import com.ecommerce.user.security.UserDetailsServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public JwtUtil testJwtUtil() {
        return mock(JwtUtil.class);
    }

    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        return mock(UserDetailsServiceImpl.class);
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter testJwtAuthenticationFilter() {
        return mock(JwtAuthenticationFilter.class);
    }

    @Bean
    @Primary
    public CognitoLogoutHandler testCognitoLogoutHandler() {
        return mock(CognitoLogoutHandler.class);
    }

    @Bean
    @Primary
    public CognitoJwtGrantedAuthoritiesConverter testCognitoJwtGrantedAuthoritiesConverter() {
        return mock(CognitoJwtGrantedAuthoritiesConverter.class);
    }
} 