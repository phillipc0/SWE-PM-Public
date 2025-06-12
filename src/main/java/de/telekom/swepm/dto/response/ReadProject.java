package de.telekom.swepm.dto.response;

import de.telekom.swepm.domain.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(value = AccessLevel.PROTECTED)
@Getter
public class ReadProject {
    private int id;
    private String name;
    private String description;
    private LocalDateTime createdOn;
    private ReadUser projectManager;
    private List<ReadUser> users;
    private Map<Status, Integer> taskStatusCount;
}
