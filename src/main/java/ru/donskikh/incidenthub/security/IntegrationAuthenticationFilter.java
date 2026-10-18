package ru.donskikh.incidenthub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class IntegrationAuthenticationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Integration-Token";
    private final IntegrationTokenProperties properties;
    private final ProblemDetailAuthenticationEntryPoint entryPoint;

    public IntegrationAuthenticationFilter(IntegrationTokenProperties properties,
                                           ProblemDetailAuthenticationEntryPoint entryPoint) {
        this.properties = properties;
        this.entryPoint = entryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !(request.getContextPath() + "/api/v1/alerts").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token != null) {
            if (!properties.matches(token)) {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, new BadCredentialsException("Invalid integration credentials"));
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    new IntegrationPrincipal("monitoring"), null,
                    List.of(new SimpleGrantedAuthority(IntegrationAuthorities.ALERT_INGEST))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
