package de.telekom.swepm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreRemove;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime startDateTime;

    private LocalDateTime completionDateTime;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<User> assignedUsers = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "parentTask_id")
    private Task parentTask;

    @Builder.Default
    @OneToMany(mappedBy = "parentTask", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<Task> subtasks = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "blockedBy", fetch = FetchType.LAZY)
    private Set<Task> blocks = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "blockedBy_blocks",
        joinColumns = @JoinColumn(name = "task_blockedBy"),
        inverseJoinColumns = @JoinColumn(name = "task_blocks")
    )
    private Set<Task> blockedBy = new HashSet<>();

    @PreRemove
    private void preRemove() {
        // Entfernen aus blockierten Aufgaben ohne direkte Modifikation der blockedBy-Sammlungen
        for (val blockedTask : new HashSet<>(blocks)) {
            blockedTask.getBlockedBy().remove(this);
        }
        blocks.clear();
        assignedUsers.clear();
        blockedBy.clear();
    }

    public boolean hasCircularDependency(Task taskToCheck) {
        if (this.equals(taskToCheck)) {
            return true;
        }
        for (Task blockedTask : blockedBy) {
            if (blockedTask.hasCircularDependency(taskToCheck)) {
                return true;
            }
        }
        return false;
    }
}
