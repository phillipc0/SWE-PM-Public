package de.telekom.swepm.security;

import de.telekom.swepm.domain.User;
import de.telekom.swepm.utils.UserUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
public class PasswordChangeSecurityFilter extends OncePerRequestFilter {

    @Autowired
    private UserUtils userUtils;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {
        User user = userUtils.getSessionUser();
        String path = request.getRequestURI();

        // Redirects users who log in to the password change site if needed
        if (user != null && user.isNewUser()
            && !path.startsWith("/assets") && !path.startsWith("/libraries") && !path.startsWith("/api")
            && !path.startsWith("/password") && !path.startsWith("android-chrome-*.png") && !path.startsWith("apple" +
            "-touch-icon.png") && !path.startsWith("favicon*.*") && !path.startsWith(
            "mstile-150x150.png") && !path.startsWith("safari-pinned-tab.svg")) {
            response.sendRedirect("/password");
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
