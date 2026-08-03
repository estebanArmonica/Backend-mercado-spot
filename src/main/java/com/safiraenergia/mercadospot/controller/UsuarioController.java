package com.safiraenergia.mercadospot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.user.ChangePasswordRequest;
import com.safiraenergia.mercadospot.dto.user.UpdateProfileRequest;
import com.safiraenergia.mercadospot.models.Usuario;
import com.safiraenergia.mercadospot.services.user.IUserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints for users")
public class UsuarioController {
    private final IUserService userService;

    @PutMapping("/updated-profile")
    public ResponseEntity<Usuario> updateProfile(@RequestBody UpdateProfileRequest updateRequest) {
        // obtemos el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Buscar el usuario por username
        Usuario currentUser = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Actualizar el perfil
        Usuario userToUpdate = new Usuario();
        userToUpdate.setUsername(updateRequest.getUsername());
        userToUpdate.setEmail(updateRequest.getEmail());
        
        Usuario updatedUser = userService.updateOwnProfile(currentUser.getId(), userToUpdate);
        
        log.info("Profile updated successfully for user: {}", username);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest passwordRequest) {
        log.info("Changing password for authenticated user");
        
        // Obtener el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Buscar el usuario por username
        Usuario currentUser = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Cambiar la contraseña
        userService.changeOwnPassword(
            currentUser.getId(), 
            passwordRequest.getCurrentPassword(), 
            passwordRequest.getNewPassword()
        );
        
        log.info("Password changed successfully for user: {}", username);
        return ResponseEntity.ok("Password changed successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<Usuario> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        Usuario currentUser = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(currentUser);
    }
}
