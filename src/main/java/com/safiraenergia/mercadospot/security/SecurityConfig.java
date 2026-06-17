package com.safiraenergia.mercadospot.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoin;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SqlInjectionFilter sqlInjectionFilter;

    
    @Autowired
    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoin, JwtAuthenticationFilter jwtAuthenticationFilter, SqlInjectionFilter sqlInjectionFilter) {
        this.jwtAuthenticationEntryPoin = jwtAuthenticationEntryPoin;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.sqlInjectionFilter = sqlInjectionFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000",
            "https://*.run.app",
            "app://"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Refresh-Token",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Refresh-Token"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoin)
            )
            .sessionManagement(sessionManagement -> 
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> 
                authorize
                    // Swagger UI endpoints
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    
                    // permisos de usuario para logueo
                    .requestMatchers("/api/v1/auth/**").permitAll()

                    // permisos de admin y backuser en el ETL
                    .requestMatchers("/api/v1/etl/upload").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/etl/progress/{jobId}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/etl/cancel/{jobId}").hasAuthority("ADMIN")

                    // permisos de admin y backuser de entidad
                    .requestMatchers("/api/v1/entidad/list-all").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/entidad/list-entidad/{id}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/entidad/rut/{rut}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/entidad/deudores").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/entidad/acreedores").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/entidad/create-new-entidad").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/entidad/update-entidad/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/entidad/delete-entidad/{id}").hasAuthority("ADMIN")

                    // permisos de admin y backuser en estado
                    .requestMatchers("/api/v1/estados/list-all").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/estados/list-estado/{id}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/estados/descripcion/{descripcion}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/estados/create-new-estado").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/estados/update-estado/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/estados/delete-estado/{id}").hasAuthority("ADMIN")

                    // permisos de admin y backuser en glosa
                    .requestMatchers("/api/v1/glosa/list-all").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/glosa/list-glosa/{id}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/glosa/search").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/glosa/create-new-glosa").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/glosa/update-glosa/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/glosa/delete-glosa/{id}").hasAuthority("ADMIN")


                    // permisos de admin y backuser en factura
                    .requestMatchers("/api/v1/factura/list-all").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/factura/list-factura/{id}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/factura/entidad/{rut}").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/factura/periodo").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/factura/search").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/factura/estadisticas").hasAnyRole("ADMIN","BACKUSER")
                    .requestMatchers("/api/v1/factura/created-factura").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/factura/update-factura/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/factura/updated-patch/{id}/estado").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/factura/delete-factura/{id}").hasAuthority("ADMIN")
                    
                    // All other requests need authentication
                    .anyRequest().authenticated()
            )
            .addFilterBefore(sqlInjectionFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
