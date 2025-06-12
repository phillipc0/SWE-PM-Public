package de.telekom.swepm.repos;

import de.telekom.swepm.domain.Role;
import de.telekom.swepm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAddressIgnoreCase(String emailAddress);

    Optional<User> findByIdAndRole(UUID id, Role role);
}
