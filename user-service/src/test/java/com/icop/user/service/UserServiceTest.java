package com.icop.user.service;

import com.icop.user.dto.RegisterRequest;
import com.icop.user.entity.User;
import com.icop.user.repository.UserRepository;
import com.icop.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("John", "Doe", "john@example.com", "password123");
    }

    @Test
    void register_shouldThrowIfEmailAlreadyExists() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldCreateUserSuccessfully() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(), anyMap())).thenReturn("mock-jwt-token");

        var response = userService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        assertEquals("john@example.com", response.email());
        assertEquals("John", response.firstName());
    }
}
