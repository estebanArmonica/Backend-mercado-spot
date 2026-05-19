package com.safiraenergia.mercadospot.security;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.models.RefreshToken;
import com.safiraenergia.mercadospot.models.Usuario;
import com.safiraenergia.mercadospot.repository.IRefreshTokenRepository;
import com.safiraenergia.mercadospot.repository.IUsuarioRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenService {
    private final IRefreshTokenRepository refreshTokenRepository;
    private final IUsuarioRepository usuarioRepository;
    private final JwtGenerador jwtGenerator;
    
    @Autowired
    public RefreshTokenService(IRefreshTokenRepository refreshTokenRepository, IUsuarioRepository usuarioRepository, JwtGenerador jwtGenerator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtGenerator = jwtGenerator;
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Delete existing refresh token
        refreshTokenRepository.deleteByUsuario(usuario);
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .usuario(usuario)
            .expiryDate(LocalDateTime.now().plusDays(7))
            .isRevoked(false)
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }
        return token;
    }
    
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setIsRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
}
