package com.icop.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The password encoder lives here rather than in SecurityConfig on purpose —
 * SecurityConfig depends on UserService (via UserDetailsService), which needs
 * this bean, and keeping it separate avoids feeding that dependency cycle.
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // bcrypt with the default strength (10) — slow enough to hurt brute
        // force, fast enough not to hurt login latency
        return new BCryptPasswordEncoder();
    }
}
