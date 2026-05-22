package com.safiraenergia.mercadospot.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;

    @Builder.Default
    private String type = "Bearer";
    
    private String username;
    private String email;
    private List<String> roles;
    private Long expiresIn;
}
