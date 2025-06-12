package de.telekom.swepm.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        PasswordChangeSecurityFilter passwordChangeSecurityFilter
    ) throws Exception {
        http
            .addFilterAfter(passwordChangeSecurityFilter, LogoutFilter.class)
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(requests -> requests
                //those pages are accessible even without login
                .requestMatchers("/assets/**", "/libraries/**", "/login*", "/error**",
                    "android-chrome-*.png", "apple-touch-icon.png", "favicon*.*", "mstile-150x150.png",
                    "safari-pinned-tab.svg", "site.webmanifest", "browserconfig.xml"
                )
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/projects")
                .permitAll()
            )
            .logout(LogoutConfigurer::permitAll)
            .sessionManagement(sessionManagement ->
                sessionManagement
                    .maximumSessions(Integer.MAX_VALUE)
                    .sessionRegistry(sessionRegistry()))
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .defaultAuthenticationEntryPointFor(
                    new ApiAuthenticationEntryPoint(),
                    new AntPathRequestMatcher("/api/**")
                )
            );

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // Session Timeout Configuration
    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent se) {
                se.getSession().setMaxInactiveInterval(14 * 24 * 60 * 60); // 14 days
            }
        };
    }

    //Fix api redirect to login page
    private static class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
        ) throws IOException {
            // Respond with a 401 status code for unauthorized access
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }
}