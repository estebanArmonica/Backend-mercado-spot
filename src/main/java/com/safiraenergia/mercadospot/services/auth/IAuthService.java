package com.safiraenergia.mercadospot.services.auth;

import com.safiraenergia.mercadospot.dto.LoginRequest;
import com.safiraenergia.mercadospot.dto.LoginResponse;
import com.safiraenergia.mercadospot.dto.RegisterRequest;

/**
 * Interface para el servicio de autenticación
 * Aplicando Principio de Segregación de Interfaces (ISP)
 */
public interface IAuthService {
    LoginResponse login(LoginRequest loginRequest);
    
    LoginResponse register(RegisterRequest registerRequest);
    
    LoginResponse refreshToken(String refreshToken);
    
    void logout(String token);
    
    boolean validateToken(String token);
}
