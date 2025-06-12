package de.telekom.swepm.dto.request;

import de.telekom.swepm.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreateUser {

    @NotBlank
    @Size(min = 1, max = 30)
    @Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\s-_]+$")
    private String name;
    @NotBlank
    @Email
    @Size(min = 1, max = 70)
    private String emailAddress;
    @NotBlank
    @Pattern(regexp = "^(?=.*\\d)(?=.*[a-zäöüß])(?=.*[A-ZÄÖÜ])(?=.*[\\^°~|!\"§$%&/()=?+*#\\-_.:,;\\\\<>'`´}\\]\\[{@]).{12,}$")
    @Size(min = 12, max = 72)
    private String password;
    @NotNull
    private Role role;
}
