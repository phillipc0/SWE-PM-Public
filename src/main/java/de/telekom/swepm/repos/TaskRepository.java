package de.telekom.swepm.repos;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.domain.Status;
import de.telekom.swepm.domain.Task;
import de.telekom.swepm.domain.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findAllByProject(Project project);

    Optional<Task> findByIdAndProject(int id, Project project);

    Optional<Task> findByProjectAndTitle(Project project, String title);

    List<Task> findAllByStatus(Status status);

    List<Task> findAllByAssignedUsersAndStatusNot(Set<User> assignedUsers, Status status, Sort dueDate);

    List<Task> findAllByTitle(String title);
}
