package de.telekom.swepm.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.telekom.swepm.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UpdateUser {
    @Size(min = 1, max = 30)
    @Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\s-_]+$")
    private String name;

    @Email
    @Size(min = 1, max = 70)
    private String emailAddress;

    private Role role;
}
