package com.icop.user.service;

import com.icop.user.dto.AuthResponse;
import com.icop.user.dto.LoginRequest;
import com.icop.user.dto.RegisterRequest;
import com.icop.user.dto.UserResponse;
import com.icop.user.entity.User;
import com.icop.user.repository.UserRepository;
import com.icop.user.security.JwtUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Registration, login, and user lookups. Doubles as the UserDetailsService
 * so Spring Security loads users straight from our repository instead of
 * needing a separate adapter class.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // @Lazy on the AuthenticationManager breaks the circular dependency:
    // SecurityConfig needs this service (as UserDetailsService) while this
    // service needs the auth manager that SecurityConfig produces.
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        // fail fast on duplicate emails — nicer error than a DB constraint blowup
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        userRepository.save(user);

        // hand back a token right away so the client doesn't need a second
        // login round-trip after registering
        String token = jwtUtil.generateToken(user, Map.of("role", user.getRole().name()));
        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        // let Spring Security do the credential check — throws if they're wrong,
        // so anything past this line is an authenticated user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user, Map.of("role", user.getRole().name()));
        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toResponse(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // email is our username — there's no separate handle
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(User user) {
        // never leak the entity (and its password hash) out of the service layer
        return new UserResponse(
                user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                user.getRole().name(), user.getCreatedAt()
        );
    }
}
