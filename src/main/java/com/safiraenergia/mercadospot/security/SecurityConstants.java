package com.safiraenergia.mercadospot.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;

@Component
public class SecurityConstants {
    
    @Value("${JWT_EXPIRATION}")
    private long jwtExpiration;

    @Value("${JWT_FIRMA}")
    private String jwtSecret;

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    public SecretKey getJwtSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
