package com.safiraenergia.mercadospot.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.safiraenergia.mercadospot.models.Usuario;
import com.safiraenergia.mercadospot.repository.IUsuarioRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService{
    
    private final IUsuarioRepository userRepo;

    @Autowired
    public CustomUserDetailsService(IUsuarioRepository userRepo){
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario user = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {} ", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        if(!user.getIsActive()){
            log.warn("User is disabled: {}", username);
            throw new UsernameNotFoundException("User is disabled: " + username);
        }

        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(rol -> new SimpleGrantedAuthority(rol.getNombre()))
            .collect(Collectors.toSet());

        log.debug("User loaded successfully: {} with roles: {}", username, authorities);

        return new User(
            user.getUsername(),
            user.getPassword(),
            authorities
        );
    }
    
}
