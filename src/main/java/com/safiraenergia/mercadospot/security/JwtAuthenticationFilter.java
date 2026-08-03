package com.safiraenergia.mercadospot.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtGenerador jwtGenerator;
    private final CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    public JwtAuthenticationFilter(JwtGenerador jwtGenerator, CustomUserDetailsService customUserDetailsService){
        this.customUserDetailsService = customUserDetailsService;
        this.jwtGenerator = jwtGenerator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtGenerator.validateToken(jwt)) {
                // Check if it's an access token, not refresh token
                if (!jwtGenerator.isRefreshToken(jwt)) {
                    String username = jwtGenerator.getUsernameFromToken(jwt);
                    log.info("JWT ok, username extraido: {}", username);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set authentication for user: {} in security context", username);
                } else {
                    log.warn("Refresh token used for authentication, rejecting");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Refresh token cannot be used for authentication");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e);
            System.out.println("Error JWT Filter: " + e);
        }
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
