package de.telekom.swepm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class CreateProject {
    @NotBlank
    @Size(min = 1, max = 42)
    private String name;

    @Size(max = 1024)
    private String description;

    @NotNull
    private UUID manager;

    private Set<UUID> users;
}
