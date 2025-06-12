package de.telekom.swepm.repos;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findAllByUsersContains(User user);

    Optional<Project> findByIdAndUsersContains(Integer id, User user);

    Optional<Project> findByName(String name);
}
