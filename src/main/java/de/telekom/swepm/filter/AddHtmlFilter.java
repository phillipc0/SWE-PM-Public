package de.telekom.swepm.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AddHtmlFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        val uri = request.getRequestURI();
        if (!uri.contains("/api/") && !uri.contains(".") && !uri.endsWith("/")) {
            // Create a new request with the modified URI
            HttpServletRequest modifiedRequest = new ModifiedRequest(request, uri + ".html");
            filterChain.doFilter(modifiedRequest, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    // Custom request wrapper to modify the URI
    private static class ModifiedRequest extends HttpServletRequestWrapper {
        private final String modifiedURI;

        public ModifiedRequest(HttpServletRequest request, String modifiedURI) {
            super(request);
            this.modifiedURI = modifiedURI;
        }

        @Override
        public String getRequestURI() {
            return modifiedURI;
        }
    }
}
