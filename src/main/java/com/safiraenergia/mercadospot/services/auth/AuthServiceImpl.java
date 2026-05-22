package com.safiraenergia.mercadospot.services.auth;

import com.safiraenergia.mercadospot.dto.auth.LoginRequest;
import com.safiraenergia.mercadospot.dto.auth.LoginResponse;
import com.safiraenergia.mercadospot.dto.auth.RegisterRequest;
import com.safiraenergia.mercadospot.models.Rol;
import com.safiraenergia.mercadospot.models.Usuario;
import com.safiraenergia.mercadospot.repository.IRolRepository;
import com.safiraenergia.mercadospot.repository.IUsuarioRepository;
import com.safiraenergia.mercadospot.security.JwtGenerador;
import com.safiraenergia.mercadospot.services.user.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements IAuthService{
    private final AuthenticationManager authenticationManager;
    private final JwtGenerador jwtGenerator;
    private final IUserService userService;
    private final IUsuarioRepository usuarioRepository;
    private final IRolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtGenerador jwtGenerator,
            IUserService userService, IUsuarioRepository usuarioRepository, IRolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
        this.userService = userService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.debug("Login attempt for user: {}", loginRequest.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = jwtGenerator.generateToken(authentication);
        String refreshToken = jwtGenerator.generateRefreshToken(loginRequest.getUsername());
        
        Usuario usuario = usuarioRepository.findByUsername(loginRequest.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        LoginResponse response = LoginResponse.builder()
            .token(token)
            .type("Bearer")
            .username(usuario.getUsername())
            .email(usuario.getEmail())
            .roles(usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toList()))
            .expiresIn(jwtGenerator.getExpirationFromToken(token))
            .build();
        
        log.info("User logged in successfully: {}", loginRequest.getUsername());
        
        return response;
    }
    
    @Override
    public LoginResponse register(RegisterRequest registerRequest) {
        log.debug("Register attempt for user: {}", registerRequest.getUsername());
        
        // Validar si el usuario ya existe
        if (userService.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        
        if (userService.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }
        
        // Crear nuevo usuario
        Usuario usuario = Usuario.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .isActive(true)
            .build();
        
        // Asignar roles
        Set<Rol> roles = new HashSet<>();
        
        if (registerRequest.getRole() != null && !registerRequest.getRole().isEmpty()) {
            Rol userRole = rolRepository.findByNombre(registerRequest.getRole())
                .orElseThrow(() -> new RuntimeException("Error: Role not found: " + registerRequest.getRole()));
            roles.add(userRole);
        } else {
            // Rol por defecto: USER
            Rol defaultRole = rolRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Default role not found"));
            roles.add(defaultRole);
        }
        
        usuario.setRoles(roles);
        
        Usuario savedUser = userService.createUser(usuario);
        
        // Generar token automáticamente después del registro
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                registerRequest.getUsername(),
                registerRequest.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = jwtGenerator.generateToken(authentication);
        String refreshToken = jwtGenerator.generateRefreshToken(registerRequest.getUsername());
        
        LoginResponse response = LoginResponse.builder()
            .token(token)
            .type("Bearer")
            .username(savedUser.getUsername())
            .email(savedUser.getEmail())
            .roles(savedUser.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toList()))
            .expiresIn(jwtGenerator.getExpirationFromToken(token))
            .build();
        
        log.info("User registered successfully: {}", registerRequest.getUsername());
        
        return response;
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.debug("Refresh token request");
        
        if (!jwtGenerator.validateToken(refreshToken) || !jwtGenerator.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        
        String username = jwtGenerator.getUsernameFromToken(refreshToken);
        
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String newToken = jwtGenerator.generateToken(
            usuario.getUsername(),
            usuario.getEmail(),
            usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toList())
        );
        
        return LoginResponse.builder()
            .token(newToken)
            .type("Bearer")
            .username(usuario.getUsername())
            .email(usuario.getEmail())
            .roles(usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toList()))
            .expiresIn(jwtGenerator.getExpirationFromToken(newToken))
            .build();
    }
    
    @Override
    public void logout(String token) {
        log.debug("Logout request");
        // Aquí puedes implementar lógica de blacklist de tokens si lo deseas
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }
    
    @Override
    public boolean validateToken(String token) {
        return jwtGenerator.validateToken(token);
    }
}
