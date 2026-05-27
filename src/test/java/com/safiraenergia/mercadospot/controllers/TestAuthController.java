package com.safiraenergia.mercadospot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safiraenergia.mercadospot.controller.AuthController;
import com.safiraenergia.mercadospot.dto.auth.LoginRequest;
import com.safiraenergia.mercadospot.dto.auth.LoginResponse;
import com.safiraenergia.mercadospot.dto.auth.RegisterRequest;
import com.safiraenergia.mercadospot.services.auth.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthController.class)
@DisplayName("Pruebas del controlador de autenticación")
public class TestAuthController {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private IAuthService authService;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    /**
     * se crean dos usuarios donde uno registra un usuario nuevo para el sistema
     * y el otro una vez el usuario creado se logueara con dicho usuario nuevo. 
    */
    @BeforeEach
    void setUp(){
        loginRequest = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        registerRequest = RegisterRequest.builder()
            .username("testuser")
            .email("newuser@example.com")
            .password("password123")
            .role("ROLE_USER")
            .build();
    }

    /**
     * Creamos un testing donde autenticamos a un usuario correctamente
     * donde se nos crea un token para seguridad de usuarios y encriptaciones de contraseñas
     * @return POST: retorna un POST entregando un JSON de los datos de autenticación
     * @throws Exception: en caso de errores lanzara una Exception (400, 404, 500)
    */
    @Test
    @DisplayName("Debe autenticar usuario correctamente")
    void shouldAuthenticateUser() throws Exception {
        when(authService.login(any(LoginRequest.class)))
            .thenReturn(LoginResponse.builder()
            .token("test-jwt-token")
            .type("Bearer")
            .username("testuser")
            .build()
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    /**
     * En caso de que las credenciales ingresadas por el usuario no sean las correctas
     * el Endpoint debe mandar una exception de credenciales inválidas
     * @throws Exception
    */
    @Test
    @DisplayName("Debe fallar autenticación con credenciales inválidas")
    void shouldFailAuthenticationWithInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Invalid credentials"));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is5xxServerError());
    }
    
    /**
     * en este endpoint tiene el rol de crear un usuario para el sistema en donde
     * al crear el usuario devolvera un objecto JSON de los datos creados
     * @return Object: devuelve un json object
     * @throws Exception: lanzara exception en caso de falta de datos
    */
    @Test
    @DisplayName("Debe Registrar a un nuevo usuario")
    void shouldRegisterNewUser() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
            .thenReturn(LoginResponse.builder()
                .token("new-user-token")
                .type("Bearer")
                .username("newuser")
                .build());
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }
}
