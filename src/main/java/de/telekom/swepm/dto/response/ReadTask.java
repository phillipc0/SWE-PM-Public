package de.telekom.swepm.dto.response;

import de.telekom.swepm.domain.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(value = AccessLevel.PROTECTED)
@Getter
public class ReadTask {
    private int id;
    private int project;
    private String title;
    private String description;
    private Status status;
    private LocalDateTime creationDateTime;
    private LocalDate dueDate;
    private LocalDateTime startDateTime;
    private LocalDateTime completionDateTime;
    private List<UUID> assignedUsers;
    private Integer parentTask;
    private List<Integer> subTasks;
    private List<Integer> blocks;
    private List<Integer> blockedBy;
}
