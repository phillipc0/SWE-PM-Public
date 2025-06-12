package de.telekom.swepm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePassword {
    @NotBlank
    @Pattern(regexp = "^(?=.*\\d)(?=.*[a-zäöüß])(?=.*[A-ZÄÖÜ])(?=.*[\\^°~|!\"§$%&/()=?+*#\\-_.:,;\\\\<>'`´}\\]\\[{@]).{12,}$")
    @Size(min = 12, max = 72)
    private String password;
}

