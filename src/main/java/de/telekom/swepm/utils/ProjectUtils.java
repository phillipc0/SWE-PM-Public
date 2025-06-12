package de.telekom.swepm.utils;

import de.telekom.swepm.domain.Project;
import de.telekom.swepm.repos.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectUtils {

    @Autowired
    private ProjectRepository projectRepository;

    public Project saveIfNotExists(Project project) {
        return projectRepository.findByName(project.getName())
            .orElseGet(() -> projectRepository.save(project));
    }
}
