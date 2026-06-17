package com.safiraenergia.mercadospot.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // configuramos orígenes permitidos
        List<String> allowedOrigins = Arrays.asList(
      "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:3000",
            "http://127.0.0.1:4200",
            "https://*.run.app",
            "https://*.onrender.com",
            "https://tu-dominio.com",
            "app://"         
        );
        config.setAllowedOrigins(allowedOrigins);

        // Métodos HTTP Permitidos
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Headers permitidos
        config.setAllowedHeaders(Arrays.asList(
      "Authorization",
            "Content-Type",
            "Accept",
            "Refresh-Token",
            "X-Requested-With",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Headers expuestas
        config.setExposedHeaders(Arrays.asList(
      "Authorization",
            "Refresh-Token"
        ));

        // Credenciales
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
