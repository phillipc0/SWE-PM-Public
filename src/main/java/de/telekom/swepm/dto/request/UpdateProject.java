package de.telekom.swepm.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UpdateProject {
    @Size(min = 1, max = 42)
    private String name;

    @Size(max = 1024)
    private String description;

    private UUID manager;

    private Set<UUID> users;
}
