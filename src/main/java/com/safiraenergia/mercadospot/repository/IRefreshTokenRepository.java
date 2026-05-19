package com.safiraenergia.mercadospot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.safiraenergia.mercadospot.models.RefreshToken;
import com.safiraenergia.mercadospot.models.Usuario;

@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
    Optional<RefreshToken> findByToken(String token);
    void deleteByUsuario(Usuario usuario);
}
