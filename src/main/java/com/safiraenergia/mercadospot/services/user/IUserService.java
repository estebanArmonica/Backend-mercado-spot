package com.safiraenergia.mercadospot.services.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.safiraenergia.mercadospot.models.Usuario;

/** 
 * Interface para el servicio de usuario
 * Aplicando Principios de Segregación de Interfaces (ISP)
*/
public interface IUserService {
    Usuario createUser(Usuario usuario);
    Usuario updateUser(Long id, Usuario usuario);
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    Page<Usuario> findAll(Pageable pageable);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void changePassword(Long id, String newPassword);
    void activateUser(Long id);
    void deactivateUser(Long id);

    // metodos para un usuario
    Usuario updateOwnProfile(Long userId, Usuario usuarioActualizado);
    void changeOwnPassword(Long userId, String currentPassword, String newPassword);
}
