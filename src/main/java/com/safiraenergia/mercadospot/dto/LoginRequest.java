package com.safiraenergia.mercadospot.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Column(name = "username", nullable = false, length = 35)
    private String username;

    @Column(name = "paasword", nullable = false, length = 35)
    private String password;
}
