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
                    // Error
                    .requestMatchers("/error").permitAll()

                    // Swagger UI endpoints
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    
                    // permisos de usuario para logueo
                    .requestMatchers("/api/v1/auth/**").permitAll()

                    // permisos de admin y backuser en el ETL
                    .requestMatchers("/api/v1/etl/upload").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/etl/progress/{jobId}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/etl/cancel/{jobId}").hasAuthority("ADMIN")

                    // permisos de admin y back user en usuarios
                    .requestMatchers("/api/v1/usuarios/updated-profile").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/usuarios/change-password").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/usuarios/me").hasAnyAuthority("ADMIN","BACK_USER")

                    // permisos de admin y backuser de entidad
                    .requestMatchers("/api/v1/entidad/list-all").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/entidad/list-entidad/{id}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/entidad/rut/{rut}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/entidad/deudores").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/entidad/acreedores").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/entidad/create-new-entidad").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/entidad/update-entidad/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/entidad/delete-entidad/{id}").hasAuthority("ADMIN")

                    // permisos de admin y backuser en estado
                    .requestMatchers("/api/v1/estados/list-all").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/estados/list-estado/{id}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/estados/descripcion/{descripcion}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/estados/create-new-estado").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/estados/update-estado/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/estados/delete-estado/{id}").hasAuthority("ADMIN")

                    // permisos de admin y backuser en glosa
                    .requestMatchers("/api/v1/glosa/list-all").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/glosa/list-glosa/{id}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/glosa/search").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/glosa/create-new-glosa").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/glosa/update-glosa/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/glosa/delete-glosa/{id}").hasAuthority("ADMIN")


                    // permisos de admin y backuser en factura
                    .requestMatchers("/api/v1/factura/list-all").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/factura/list-factura/{id}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/factura/entidad/{rut}").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/factura/periodo").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/factura/search").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/factura/estadisticas").hasAnyAuthority("ADMIN","BACK_USER")
                    .requestMatchers("/api/v1/factura/created-factura").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/factura/update-factura/{id}").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/factura/updated-patch/{id}/estado").hasAuthority("ADMIN")
                    .requestMatchers("/api/v1/factura/delete-factura/{id}").hasAuthority("ADMIN")
                    
                    // All other requests need authentication
                    .anyRequest().authenticated()
            )
            //.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            //.addFilterAfter(sqlInjectionFilter, JwtAuthenticationFilter.class);
            .addFilterBefore(sqlInjectionFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
