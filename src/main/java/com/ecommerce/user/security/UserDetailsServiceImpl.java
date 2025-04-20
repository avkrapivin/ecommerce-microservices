package com.ecommerce.user.security;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String cognitoId) throws UsernameNotFoundException {
        User user = userRepository.findByCognitoId(cognitoId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with cognitoId: " + cognitoId));

        return new org.springframework.security.core.userdetails.User(
                user.getCognitoId(),
                "",
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getAuthority()))
        );
    }
} 