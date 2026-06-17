package com.safiraenergia.mercadospot.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.auth.LoginRequest;
import com.safiraenergia.mercadospot.dto.auth.LoginResponse;
import com.safiraenergia.mercadospot.services.auth.IAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for authentication and user management")
public class AuthController {
    private final IAuthService authService;

    @Autowired
    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate used and return JWT Token",
        description = "Autenticación que retorna un token para un usuario",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Autenticación requiere de un username y password",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Autenticación existosa",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class)
                )
            )
        }
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request for user: {}", loginRequest.getUsername());
        LoginResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token using refresh token")
    public ResponseEntity<LoginResponse> resfreshToken(@RequestHeader("Refresh-Token") String refreshtoken) {
        log.info("Refresh token request");
        LoginResponse response = authService.refreshToken(refreshtoken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Autenticación que retorna un token para un usuario",
        tags = {"Authentication"},
        parameters = {
            @Parameter(
                name = "token",
                description = "El token es requerido para cerrar la sesión de usuario",
                example = "AJBKSDFBEWN~SSSSSS",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Sesión cerrada",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class)
                )
            )
        }
    )
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout request");
        
        // Eliminar el prefijo "Bearer " si existe
        String jwt = token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
        
        authService.logout(jwt);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(response);
    }
}
