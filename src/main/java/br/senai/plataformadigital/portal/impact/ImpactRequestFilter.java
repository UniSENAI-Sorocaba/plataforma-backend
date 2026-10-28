package br.senai.plataformadigital.portal.impact;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ImpactRequestFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/v1/impacts");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlation = request.getHeader("X-Correlation-Id");
        if (correlation == null || !correlation.matches("[A-Za-z0-9._-]{1,64}")) correlation = UUID.randomUUID().toString();
        request.setAttribute("impact.correlation", correlation);
        response.setHeader("X-Correlation-Id", correlation);
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
