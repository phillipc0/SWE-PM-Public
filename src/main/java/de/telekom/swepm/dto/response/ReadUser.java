package de.telekom.swepm.dto.response;

import de.telekom.swepm.domain.Role;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(value = AccessLevel.PROTECTED)
@Getter
public class ReadUser {
    private UUID id;
    private String name;
    private String emailAddress;
    private Role role;
    private Boolean isNewUser;
    private List<Integer> projectIds;
}
