package de.telekom.swepm.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class CreateTask {
    @NotNull
    private int project;

    @NotBlank
    @Size(min = 1, max = 128)
    private String title;

    @NotBlank
    @Size(max = 1024)
    private String description;

    @NotNull
    @FutureOrPresent
    private LocalDate dueDate;

    private Set<UUID> assignedUsers;

    private Integer parentTask;

    private Set<Integer> blockedBy;
}
