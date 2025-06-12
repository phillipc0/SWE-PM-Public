package de.telekom.swepm.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import de.telekom.swepm.domain.Status;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Important Note: Adding {@code @Builder} Annotation will not work with the boolean flags
 **/
@Getter
@Setter
public class UpdateTask {
    @Size(max = 128)
    private String title;
    @Size(max = 1024)
    private String description;
    private Status status;
    @FutureOrPresent
    private LocalDate dueDate;
    private Set<UUID> assignedUsers;
    private Integer parentTask;
    private Set<Integer> subTasks;
    private Set<Integer> blockedBy;

    // Flags to track if fields were explicitly set (important to differentiate between null and not set)
    private boolean titleSet;
    private boolean descriptionSet;
    private boolean statusSet;
    private boolean dueDateSet;
    private boolean assignedUsersSet;
    private boolean parentTaskSet;
    private boolean subTasksSet;
    private boolean blockedBySet;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.setTitleSet(true);
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.setDescriptionSet(true);
    }

    @JsonSetter("status")
    public void setStatus(Status status) {
        this.status = status;
        this.setStatusSet(true);
    }

    @JsonSetter("dueDate")
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        this.setDueDateSet(true);
    }

    @JsonSetter("assignedUsers")
    public void setAssignedUsers(Set<UUID> assignedUsers) {
        this.assignedUsers = assignedUsers;
        this.setAssignedUsersSet(true);
    }

    @JsonSetter("parentTask")
    public void setParentTask(Integer parentTask) {
        this.parentTask = parentTask;
        this.setParentTaskSet(true);
    }

    @JsonSetter("subTasks")
    public void setSubTasks(Set<Integer> subTasks) {
        this.subTasks = subTasks;
        this.setSubTasksSet(true);
    }

    @JsonSetter("blockedBy")
    public void setBlockedBy(Set<Integer> blockedBy) {
        this.blockedBy = blockedBy;
        this.setBlockedBySet(true);
    }
}
