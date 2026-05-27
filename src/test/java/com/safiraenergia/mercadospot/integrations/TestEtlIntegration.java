package com.safiraenergia.mercadospot.integrations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.safiraenergia.mercadospot.dto.auth.LoginRequest;
import com.safiraenergia.mercadospot.dto.auth.LoginResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Pruebas de integración E2E")
public class TestEtlIntegration {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // login para obtener token
        LoginRequest loginRequest = LoginRequest.builder()
            .username("admin")
            .password("admin123")
            .build();
        
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            LoginResponse.class
        );
        
        if (loginResponse.getBody() != null) {
            jwtToken = loginResponse.getBody().getToken();
        }
    }

    @Test
    @DisplayName("Flujo completo ETL: upload, progress, validación")
    void testCompleteETLFlow() throws Exception {
        // 1. Subir archivo
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("test-data.xlsx"));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> uploadResponse = restTemplate.postForEntity(
            "/api/v1/etl/upload",
            requestEntity,
            String.class
        );
        
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(uploadResponse.getBody()).contains("Archivo recibido");
        
        // 2. Verificar facturas cargadas
        HttpEntity<Void> getHeaders = new HttpEntity<>(headers);
        
        ResponseEntity<String> facturasResponse = restTemplate.exchange(
            "/api/v1/facturas",
            HttpMethod.GET,
            getHeaders,
            String.class
        );
        
        assertThat(facturasResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
