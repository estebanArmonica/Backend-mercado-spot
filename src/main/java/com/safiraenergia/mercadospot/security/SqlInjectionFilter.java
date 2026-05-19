package com.safiraenergia.mercadospot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class SqlInjectionFilter extends OncePerRequestFilter{

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(\\bSELECT\\b|\\bINSERT\\b|\\bUPDATE\\b|\\bDELETE\\b|\\bDROP\\b|\\bUNION\\b|\\bJOIN\\b|--|;|\\bOR\\b|\\bAND\\b).*",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Validate request parameters
        if (containsSqlInjection(request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Potential SQL injection detected");
            return;
        }
        
        filterChain.doFilter(new SqlSafeRequestWrapper(request), response);
    }

    private boolean containsSqlInjection(HttpServletRequest request) {
        // Check query parameters
        if (request.getQueryString() != null && 
            SQL_INJECTION_PATTERN.matcher(request.getQueryString()).matches()) {
            return true;
        }
        
        // Check request parameters
        for (String param : request.getParameterMap().keySet()) {
            String[] values = request.getParameterValues(param);
            for (String value : values) {
                if (value != null && SQL_INJECTION_PATTERN.matcher(value).matches()) {
                    return true;
                }
            }
        }
        
        return false;
    }

    // Wrapper to sanitize input
    private static class SqlSafeRequestWrapper extends HttpServletRequestWrapper {
        public SqlSafeRequestWrapper(HttpServletRequest request) {
            super(request);
        }
        
        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitizeInput(values[i]);
            }
            return sanitized;
        }
        
        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitizeInput(value);
        }
        
        private String sanitizeInput(String input) {
            if (input == null) return null;
            // Remove potential SQL injection characters
            return input.replaceAll("(['\";])", "");
        }
    }
}
