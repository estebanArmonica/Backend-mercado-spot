package com.safiraenergia.mercadospot.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.safiraenergia.mercadospot.repository.IUsuarioRepository;
import com.safiraenergia.mercadospot.security.CustomUserDetailsService;
import com.safiraenergia.mercadospot.security.JwtGenerador;
import com.safiraenergia.mercadospot.security.SecurityConstants;

@TestConfiguration
public class TestSecurityConfig {

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    @Primary
    public SecurityConstants testSecurityConstants() {
        SecurityConstants constants = new SecurityConstants();

        return constants;
    }

    @Bean
    @Primary
    public JwtGenerador testJwtGenerador(SecurityConstants securityConstants) {
        return new JwtGenerador(securityConstants); 
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return customUserDetailsService;
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
