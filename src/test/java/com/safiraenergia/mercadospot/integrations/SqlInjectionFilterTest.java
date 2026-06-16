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
import static org.mockito.Mockito.any;
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
    }

    // ==================== PRUEBAS DE ATAQUES SQL ====================

    @Test
    @DisplayName("Debe bloquear ataque SQL con SELECT")
    void shouldBlockSqlInjectionWithSelect() throws Exception {
        request.setParameter("query", "SELECT * FROM usuarios WHERE id = 1");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(response.getContentAsString()).contains("Potential SQL injection detected");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con DROP")
    void shouldBlockSqlInjectionWithDrop() throws Exception {
        request.setParameter("table", "DROP TABLE factura");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con UNION")
    void shouldBlockSqlInjectionWithUnion() throws Exception {
        request.setParameter("id", "1 UNION SELECT * FROM usuarios");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con OR 1=1")
    void shouldBlockSqlInjectionWithOr1Equals1() throws Exception {
        request.setParameter("id", "1 OR 1=1");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL con comentarios")
    void shouldBlockSqlInjectionWithComments() throws Exception {
        request.setParameter("id", "1; --");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear ataque SQL en query string")
    void shouldBlockSqlInjectionInQueryString() throws Exception {
        request.setQueryString("id=1 OR 1=1");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ==================== PRUEBAS DE SANITIZACIÓN ====================

    @Test
    @DisplayName("Debe sanitizar caracteres peligrosos")
    void shouldSanitizeDangerousCharacters() throws Exception {
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        
        // ✅ Usar solo comillas simples (no contiene ; que es lo que activa el bloqueo)
        request.setParameter("name", "Juan'");
        
        filter.doFilter(request, response, filterChain);
        
        // ✅ La petición debería continuar (no hay palabras SQL ni ;)
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        
        // Verificar que los caracteres peligrosos fueron removidos
        HttpServletRequest sanitizedRequest = requestCaptor.getValue();
        String sanitizedValue = sanitizedRequest.getParameter("name");
        
        assertThat(sanitizedValue).doesNotContain("'");
        assertThat(sanitizedValue).doesNotContain("\"");
    }

    @Test
    @DisplayName("Debe sanitizar caracteres peligrosos en múltiples parámetros")
    void shouldSanitizeMultipleDangerousCharacters() throws Exception {
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        
        // ✅ Usar comillas simples, SIN punto y coma
        request.setParameter("name", "Juan'");
        request.setParameter("description", "Test with quote'");
        
        filter.doFilter(request, response, filterChain);
        
        // ✅ La petición debería continuar
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        
        // Verificar que los caracteres peligrosos fueron removidos
        HttpServletRequest sanitizedRequest = requestCaptor.getValue();
        
        String name = sanitizedRequest.getParameter("name");
        String description = sanitizedRequest.getParameter("description");
        
        assertThat(name).doesNotContain("'");
        assertThat(name).doesNotContain("\"");
        
        assertThat(description).doesNotContain("'");
        assertThat(description).doesNotContain("\"");
    }

    @Test
    @DisplayName("Debe sanitizar comillas dobles")
    void shouldSanitizeDoubleQuotes() throws Exception {
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        
        request.setParameter("name", "Juan\"");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        
        HttpServletRequest sanitizedRequest = requestCaptor.getValue();
        String sanitizedValue = sanitizedRequest.getParameter("name");
        
        assertThat(sanitizedValue).doesNotContain("\"");
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con DROP")
    void shouldBlockSqlInjectionWithDropTable() throws Exception {
        request.setParameter("query", "DROP TABLE usuarios");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con INSERT")
    void shouldBlockSqlInjectionWithInsert() throws Exception {
        request.setParameter("query", "INSERT INTO usuarios VALUES ('admin', 'password')");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con UPDATE")
    void shouldBlockSqlInjectionWithUpdate() throws Exception {
        request.setParameter("query", "UPDATE usuarios SET password='123'");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con DELETE")
    void shouldBlockSqlInjectionWithDelete() throws Exception {
        request.setParameter("query", "DELETE FROM usuarios");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe bloquear SQL Injection con punto y coma")
    void shouldBlockSqlInjectionWithSemicolon() throws Exception {
        request.setParameter("query", "1; DROP TABLE usuarios");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe permitir datos normales sin SQL Injection")
    void shouldAllowNormalData() throws Exception {
        request.setParameter("id", "12345");
        request.setParameter("nombre", "Empresa Test");
        request.setParameter("email", "test@empresa.com");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe permitir texto con palabras que contienen 'or' dentro")
    void shouldAllowTextWithOrInsideWord() throws Exception {
        request.setParameter("name", "color");
        request.setParameter("description", "normal word");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe permitir texto con palabras que contienen 'and' dentro")
    void shouldAllowTextWithAndInsideWord() throws Exception {
        request.setParameter("name", "sandwich");
        request.setParameter("description", "landscape");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe manejar request sin parámetros")
    void shouldHandleRequestWithoutParameters() throws Exception {
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Debe permitir parámetros vacíos")
    void shouldAllowEmptyParameters() throws Exception {
        request.setParameter("id", "");
        request.setParameter("name", "");
        
        filter.doFilter(request, response, filterChain);
        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(filterChain, times(1)).doFilter(any(), any());
    }
}