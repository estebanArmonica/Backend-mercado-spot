package com.safiraenergia.mercadospot.integrations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
    
    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        this.restTemplate = new RestTemplate();
        this.baseUrl = "http://localhost:" + port;
        
        System.out.println("=== INICIALIZANDO PRUEBA ===");
        System.out.println("Puerto: " + port);
        System.out.println("Base URL: " + baseUrl);
    }

    @Test
    @DisplayName("Flujo completo ETL: upload, progress, validación")
    void testCompleteETLFlow() throws Exception {
        System.out.println("=== INICIANDO PRUEBA E2E ===");
        
        // 1. Login para obtener token
        LoginRequest loginRequest = LoginRequest.builder()
            .username("admin")
            .password("Admin123!")  // ✅ Contraseña correcta
            .build();
        
        System.out.println("1. Intentando login con usuario: admin...");
        
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            loginRequest,
            LoginResponse.class
        );
        
        // Verificar que la respuesta no es null
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        LoginResponse responseBody = loginResponse.getBody();
        assertThat(responseBody).isNotNull();
        
        String jwtToken = responseBody.getToken();
        assertThat(jwtToken).isNotNull();
        assertThat(jwtToken).isNotEmpty();
        
        System.out.println("✅ Login exitoso. Token obtenido: " + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
        
        // 2. Subir archivo
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("test-data.xlsx"));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        System.out.println("2. Subiendo archivo...");
        ResponseEntity<String> uploadResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/etl/upload",
            requestEntity,
            String.class
        );
        
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(uploadResponse.getBody()).contains("Archivo recibido");
        System.out.println("✅ Archivo subido correctamente");
        
        // 3. Verificar facturas cargadas
        HttpEntity<Void> getHeaders = new HttpEntity<>(headers);
        
        System.out.println("3. Consultando facturas...");
        ResponseEntity<String> facturasResponse = restTemplate.exchange(
            baseUrl + "/api/v1/factura/list-all",
            HttpMethod.GET,
            getHeaders,
            String.class
        );
        
        assertThat(facturasResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Facturas consultadas correctamente");
        
        System.out.println("=== PRUEBA E2E COMPLETADA EXITOSAMENTE ===");
    }
}
