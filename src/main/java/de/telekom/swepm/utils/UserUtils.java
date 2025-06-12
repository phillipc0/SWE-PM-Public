package de.telekom.swepm.utils;

import de.telekom.swepm.domain.Role;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.repos.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.google.common.collect.MoreCollectors.onlyElement;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
public class UserUtils {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    //Returns the user of the current request
    public User getSessionUser() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        val email = authentication.getName();
        return userRepository.findByEmailAddressIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    }

    //Returns the role of the current user by checking the authentication and not the database (faster)
    public Role getSessionRole() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        val role = authentication.getAuthorities().stream().collect(onlyElement()).getAuthority();

        return Role.valueOf(role);
    }

    public User saveIfNotExists(User user) {
        return userRepository.findByEmailAddressIgnoreCase(user.getEmailAddress())
            .orElseGet(() -> userRepository.save(user));
    }

    public User getUser(String user) {
        return userRepository.findByEmailAddressIgnoreCase(user)
            .orElse(null);
    }

    public void invalidateUserSession(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof org.springframework.security.core.userdetails.User user &&
                (user.getUsername().equals(username))) {
                List<SessionInformation> sessionsInfo = sessionRegistry.getAllSessions(user, false);
                for (SessionInformation sessionInfo : sessionsInfo) {
                    sessionInfo.expireNow();
                    log.info("Expired session " + sessionInfo.getLastRequest() + " of user: " + username);
                }
            }
        }
    }
}

