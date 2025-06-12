package de.telekom.swepm;

import de.telekom.swepm.domain.User;
import de.telekom.swepm.utils.UserUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static de.telekom.swepm.domain.Role.PROJECT_MANAGER;

@Service
public class InitialSetup {

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void setup() {
        createAdmin();
    }

    private void createAdmin() {
        userUtils.saveIfNotExists(
            User.builder()
                .name("Projio Admin")
                .emailAddress("projio")
                .role(PROJECT_MANAGER)
                .passwordHash(passwordEncoder.encode("projio"))
                .isNewUser(false)
                .build());
    }
}
