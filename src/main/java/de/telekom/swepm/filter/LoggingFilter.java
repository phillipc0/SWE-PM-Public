package de.telekom.swepm.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws
        ServletException, IOException {
        val uri = request.getRequestURI();
        if (!uri.startsWith("/assets") && !uri.startsWith("/libraries")) {
            val auth = SecurityContextHolder.getContext().getAuthentication();
            val queryString = request.getQueryString() == null ? "" : "?" + request.getQueryString();
            log.info(request.getMethod() + " " + uri + queryString +
                " visited by \"" + auth.getName() + "\" with role " + auth.getAuthorities());
        }
        filterChain.doFilter(request, response);
    }
}

