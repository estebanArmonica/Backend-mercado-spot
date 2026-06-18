package com.safiraenergia.mercadospot.integrations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.safiraenergia.mercadospot.security.SqlInjectionFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@DisplayName("Pruebas del filtro anti SQL Injection")
class SqlInjectionFilterTest {

    private SqlInjectionFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new SqlInjectionFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        System.out.println("\n==================================================");
        System.out.println("   Iniciando pruebas de ataque con SQL Injection");
        System.out.println("==================================================\n");
    }

    // ==================== PRUEBAS DE ATAQUES SQL ====================

    @Test
    @DisplayName("Debe bloquear ataque SQL con SELECT")
    void shouldBlockSqlInjectionWithSelect() throws Exception {
        System.out.println("TEST: Debe bloquear ataque de SQL con SELECT");
        System.out.println("parameter: query");

        request.setParameter("query", "SELECT * FROM usuario WHERE id = 1");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(response.getContentAsString()).contains("Potential SQL injection detected");
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque usando SELECT bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con DROP")
    void shouldBlockSqlInjectionWithDrop() throws Exception {
        System.out.println("TEST: Debe bloquear ataque de SQL con DROP");
        System.out.println("parameter: table");

        request.setParameter("table", "DROP TABLE factura");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque usando DROP bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con UNION")
    void shouldBlockSqlInjectionWithUnion() throws Exception {
        System.out.println("TEST: Debe bloquear ataque de SQL con UNION");
        System.out.println("parameter: id de la tabla");

        request.setParameter("id", "1 UNION SELECT * FROM usuario");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque usando UNION bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con OR 1=1")
    void shouldBlockSqlInjectionWithOr1Equals1() throws Exception {
        System.out.println("TEST: Debe bloquear ataque de SQL con OR");
        System.out.println("parameter: id de la tabla");

        request.setParameter("id", "1 OR 1=1");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque usando OR bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con comentarios")
    void shouldBlockSqlInjectionWithComments() throws Exception {
        System.out.println("TEST: Debe bloquear ataque de SQL por comentarios");
        System.out.println("parameter: --");

        request.setParameter("id", "1; --");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque usando comentarios bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL en query string")
    void shouldBlockSqlInjectionInQueryString() throws Exception {
        System.out.println("TEST: Debe bloquear ataque de SQL en query String");
        System.out.println("parameter: Strings");

        request.setQueryString("id=1 OR 1=1");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque usando query String bloqueado correctamente\n");
    }

    // ==================== PRUEBAS DE SANITIZACIÓN ====================

    @Test
    @DisplayName("Debe sanitizar caracteres peligrosos")
    void shouldSanitizeDangerousCharacters() throws Exception {
        System.out.println("Test: Debe sanitizar caracteres peligrosos");
        System.out.println("Parámetro: name");
        System.out.println("Este input NO contiene palabras SQL, solo comillas simples");


        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        
        // ✅ Usar solo comillas simples (no contiene ; que es lo que activa el bloqueo)
        request.setParameter("name", "Juan'");
        
        filter.doFilter(request, response, filterChain);
        
        // ✅ La petición debería continuar (no hay palabras SQL ni ;)
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        
        verify(filterChain).doFilter(requestCaptor.capture(), any());

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        // Verificar que los caracteres peligrosos fueron removidos
        HttpServletRequest sanitizedRequest = requestCaptor.getValue();
        String sanitizedValue = sanitizedRequest.getParameter("name");

        System.out.println("Valor original: 'Juan''");
        System.out.println("Valor sanitizado: '" + sanitizedValue + "'");
        
        assertThat(sanitizedValue).doesNotContain("'");
        assertThat(sanitizedValue).doesNotContain("\"");

        System.out.println("Caracteres peligrosos sanitizados correctamente\n");
    }

    @Test
    @DisplayName("Debe sanitizar caracteres peligrosos en múltiples parámetros")
    void shouldSanitizeMultipleDangerousCharacters() throws Exception {
        System.out.println("Test: Debe sanitizar caracteres peligrosos en múltiples parámetros");
        System.out.println("Parámetros: name = 'Juan'', description = 'Test with quote''");
        System.out.println("Estos inputs NO contienen palabras SQL, solo comillas simples");

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        
        // ✅ Usar comillas simples, SIN punto y coma
        request.setParameter("name", "Juan'");
        request.setParameter("description", "Test with quote'");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        // ✅ La petición debería continuar
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        
        // Verificar que los caracteres peligrosos fueron removidos
        HttpServletRequest sanitizedRequest = requestCaptor.getValue();
        
        String name = sanitizedRequest.getParameter("name");
        String description = sanitizedRequest.getParameter("description");

        System.out.println("Valor original name: 'Juan'' → sanitizado: '" + name + "'");
        System.out.println("Valor original description: 'Test with quote'' → sanitizado: '" + description + "'");
        
        assertThat(name).doesNotContain("'");
        assertThat(name).doesNotContain("\"");
        
        assertThat(description).doesNotContain("'");
        assertThat(description).doesNotContain("\"");

        System.out.println("Múltiples parámetros sanitizados correctamente\n");
    }

    @Test
    @DisplayName("Debe sanitizar comillas dobles")
    void shouldSanitizeDoubleQuotes() throws Exception {
        System.out.println("Test: Debe sanitizar comillas dobles");
        System.out.println("Parámetro: name = 'Juan\"'");
        System.out.println("Este input NO contiene palabras SQL, solo comillas dobles");

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        
        request.setParameter("name", "Juan\"");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        
        HttpServletRequest sanitizedRequest = requestCaptor.getValue();
        String sanitizedValue = sanitizedRequest.getParameter("name");

        System.out.println("Valor original: 'Juan\"' → sanitizado: '" + sanitizedValue + "'");
        
        assertThat(sanitizedValue).doesNotContain("\"");

        System.out.println("Comillas dobles sanitizadas correctamente\n");
    }

    // ==================== PRUEBAS DE BLOQUEO DE PALABRAS SQL ====================

    @Test
    @DisplayName("Debe bloquear SQL Injection con DROP")
    void shouldBlockSqlInjectionWithDropTable() throws Exception {
        System.out.println("Test: Debe bloquear SQL Injection con DROP");
        System.out.println("Parámetro: query");

        request.setParameter("query", "DROP TABLE usuarios");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque DROP TABLE bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con INSERT")
    void shouldBlockSqlInjectionWithInsert() throws Exception {
        System.out.println("Test: Debe bloquear SQL Injection con INSERT");
        System.out.println("Parámetro: query = 'INSERT INTO usuarios VALUES (\"admin\", \"password\")'");
        
        request.setParameter("query", "INSERT INTO usuarios VALUES ('admin', 'password')");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque INSERT bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con UPDATE")
    void shouldBlockSqlInjectionWithUpdate() throws Exception {
        System.out.println("Test: Debe bloquear SQL Injection con UPDATE");
        System.out.println("Parámetro: query = 'UPDATE usuarios SET password=\"123\"'");

        request.setParameter("query", "UPDATE usuarios SET password='123'");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque UPDATE bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con DELETE")
    void shouldBlockSqlInjectionWithDelete() throws Exception {
        System.out.println("Test: Debe bloquear SQL Injection con DELETE");
        System.out.println("Parámetro: query = 'DELETE FROM usuarios'");

        request.setParameter("query", "DELETE FROM usuarios");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque DELETE bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con punto y coma")
    void shouldBlockSqlInjectionWithSemicolon() throws Exception {
        System.out.println("Test: Debe bloquear SQL Injection con punto y coma");
        System.out.println("Parámetro: query = '1; DROP TABLE usuarios'");

        request.setParameter("query", "1; DROP TABLE usuarios");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response: " + response.getContentAsString());
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());

        System.out.println("Ataque con punto y coma bloqueado correctamente\n");
    }

    @Test
    @DisplayName("Debe permitir datos normales sin SQL Injection")
    void shouldAllowNormalData() throws Exception {
        System.out.println("Test: Debe permitir datos normales sin SQL Injection");
        System.out.println("Parámetros: id = '12345', nombre = 'Empresa Test', email = 'test@empresa.com'");
        System.out.println("Estos inputs son completamente seguros");

        request.setParameter("id", "12345");
        request.setParameter("nombre", "Empresa Test");
        request.setParameter("email", "test@empresa.com");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());

        System.out.println("Datos normales permitidos correctamente\n");
    }

    @Test
    @DisplayName("Debe permitir texto con palabras que contienen 'or' dentro")
    void shouldAllowTextWithOrInsideWord() throws Exception {
        System.out.println("Test: Debe permitir texto con palabras que contienen 'or' dentro");
        System.out.println("Parámetro: name = 'color'");
        System.out.println("'color' contiene 'or' pero no es un ataque SQL");

        request.setParameter("name", "color");
        request.setParameter("description", "normal word");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());

        System.out.println("Palabra con 'or' dentro permitida correctamente\n");
    }

    @Test
    @DisplayName("Debe permitir texto con palabras que contienen 'and' dentro")
    void shouldAllowTextWithAndInsideWord() throws Exception {
        System.out.println("Test: Debe permitir texto con palabras que contienen 'and' dentro");
        System.out.println("Parámetro: name = 'sandwich'");
        System.out.println("'sandwich' contiene 'and' pero no es un ataque SQL");

        request.setParameter("name", "sandwich");
        request.setParameter("description", "landscape");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());

        System.out.println("Palabra con 'and' dentro permitida correctamente\n");
    }

    @Test
    @DisplayName("Debe manejar request sin parámetros")
    void shouldHandleRequestWithoutParameters() throws Exception {
        System.out.println("Test: Debe manejar request sin parámetros");
        System.out.println("Request sin ningún parámetro");

        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());

        System.out.println("Request sin parámetros manejado correctamente\n");
    }

    @Test
    @DisplayName("Debe permitir parámetros vacíos")
    void shouldAllowEmptyParameters() throws Exception {
        System.out.println("Test: Debe permitir parámetros vacíos");
        System.out.println("Parámetros: id = '', name = ''");
        System.out.println("Parámetros vacíos no representan una amenaza");

        request.setParameter("id", "");
        request.setParameter("name", "");
        
        filter.doFilter(request, response, filterChain);

        System.out.println("Status Code: " + response.getStatus());
        System.out.println("La petición fue permitida (Status: 200)");
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());

        System.out.println("Parámetros vacíos permitidos correctamente\n");
    }
}