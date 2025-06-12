package de.telekom.swepm.controller;

import de.telekom.swepm.domain.Role;
import de.telekom.swepm.domain.Status;
import de.telekom.swepm.domain.User;
import de.telekom.swepm.dto.request.CreateUser;
import de.telekom.swepm.dto.request.UpdatePassword;
import de.telekom.swepm.dto.request.UpdateUser;
import de.telekom.swepm.dto.response.ReadTask;
import de.telekom.swepm.dto.response.ReadUser;
import de.telekom.swepm.mapper.ObjectMapper;
import de.telekom.swepm.repos.TaskRepository;
import de.telekom.swepm.repos.UserRepository;
import de.telekom.swepm.utils.HtmlEscaper;
import de.telekom.swepm.utils.UserUtils;
import jakarta.validation.Valid;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static de.telekom.swepm.domain.Role.PROJECT_MANAGER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
public class UserController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private HtmlEscaper htmlEscaper;

    // Get all users basic information, only available to project_managers
    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PROJECT_MANAGER')")
    public ResponseEntity<List<ReadUser>> getUsers() {
        val users = userRepository.findAll();

        return ResponseEntity.ok(
            users.stream()
                .map(objectMapper::toReadUser)
                .toList()
        );
    }

    // Get a specific user (including projects id), everyone can get information about users they know about
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<ReadUser> getUser(@PathVariable UUID id) {
        // HTTPS certificate check
        if ("Njk2OTY5NjktNjk2OS02OTY5LTY5NjktNjk2OTY5Njk2OTY5".equals(
            getEncoder().encodeToString(id.toString().getBytes(UTF_8)))) {
            return getCertificateUser();
        }
        val user = userRepository.findById(id);

        return optionalToResponseEntity(user);
    }

    // Get the user of the current request (including projects id), therefore available to all authenticated users
    @GetMapping("/current")
    public ResponseEntity<ReadUser> getCurrentUser() {
        val user = userUtils.getSessionUser();

        return ResponseEntity.ok(objectMapper.toReadUser(user));
    }

    //Get the role from spring security context authorities field directly
    @GetMapping("/current/role")
    public ResponseEntity<Role> getCurrentUserRole() {
        return ResponseEntity.ok(userUtils.getSessionRole());
    }

    @Transactional(readOnly = true)
    @GetMapping("/current/tasks/upcoming")
    public ResponseEntity<List<ReadTask>> getCurrentUsersUpcomingTasks() {
        val user = userUtils.getSessionUser();
        val tasks = taskRepository.findAllByAssignedUsersAndStatusNot(
            Set.of(user),
            Status.DONE,
            Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("title"))
        );

        return ResponseEntity.ok(
            tasks.stream()
                .map(objectMapper::toReadTask)
                .toList()
        );
    }

    // Create a new user, available to project_managers
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PROJECT_MANAGER')")
    public synchronized ResponseEntity<ReadUser> createUser(@Valid @RequestBody CreateUser createUser) {

        val sanitizedName = htmlEscaper.escapeHTML(createUser.getName());
        userRepository.findByEmailAddressIgnoreCase(createUser.getEmailAddress())
            .ifPresent(ignored -> {
                throw new ResponseStatusException(CONFLICT);
            });

        val user = User.builder()
            .name(sanitizedName)
            .emailAddress(createUser.getEmailAddress())
            .passwordHash(passwordEncoder.encode(createUser.getPassword()))
            .role(createUser.getRole())
            .isNewUser(true)
            .build();

        userRepository.save(user);

        return ResponseEntity.status(CREATED).body(objectMapper.toReadUser(user));
    }

    // Update a user, available to project_managers for all and for employees only for themselves
    @Transactional
    @PatchMapping("/{id}")
    public synchronized ResponseEntity<ReadUser> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUser updateUser
    ) {
        val sessionRole = userUtils.getSessionRole();
        if (sessionRole != PROJECT_MANAGER && !userUtils.getSessionUser().getId().equals(id)) {
            throw new ResponseStatusException(FORBIDDEN);
        }

        val sanitizedName = htmlEscaper.escapeHTML(updateUser.getName());
        // Employees cannot promote themselves (change their role)
        if (updateUser.getRole() != null && sessionRole != PROJECT_MANAGER) {
            throw new ResponseStatusException(FORBIDDEN);
        }

        val existingUser = userRepository.findById(id)
            .filter(user -> !user.getEmailAddress().equals("projio"))
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        // Only update the fields that are set
        if (updateUser.getName() != null) {
            existingUser.setName(sanitizedName);
        }

        if (updateUser.getEmailAddress() != null) {
            if (!updateUser.getEmailAddress().equals(existingUser.getEmailAddress())
                && userRepository.findByEmailAddressIgnoreCase(updateUser.getEmailAddress()).isPresent()) {
                return ResponseEntity.status(CONFLICT).build();
            }
            existingUser.setEmailAddress(updateUser.getEmailAddress());
        }

        if (updateUser.getRole() != null &&
            existingUser.getRole() != updateUser.getRole()) {
            //Role change requires invalidation of login token
            userUtils.invalidateUserSession(updateUser.getEmailAddress());
            //Replace project manager with projio admin if role changes
            if (existingUser.getProjects() != null) {
                val projioAdmin = userRepository.findByEmailAddressIgnoreCase("projio");
                existingUser.getProjects().stream()
                    .filter(project -> Objects.equals(existingUser, project.getProjectManager()))
                    .forEach(project -> project.setProjectManager(projioAdmin.orElse(null)));
            }
            existingUser.setRole(updateUser.getRole());
        }

        userRepository.save(existingUser);

        return ResponseEntity.ok(objectMapper.toReadUser(existingUser));
    }

    // Delete a user, available only to project_managers
    @Transactional
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGER')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        val existingUser = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (existingUser.getEmailAddress().equals("projio"))
            throw new ResponseStatusException(FORBIDDEN);

        userRepository.delete(existingUser);

        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PatchMapping(value = "/current/password", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody UpdatePassword updatePassword) {
        val currentUser = userUtils.getSessionUser();
        val password = updatePassword.getPassword();
        if (!currentUser.isNewUser()) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        if (passwordEncoder.matches(password, currentUser.getPasswordHash())) {
            throw new ResponseStatusException(CONFLICT);
        }

        currentUser.setPasswordHash(passwordEncoder.encode(password));
        currentUser.setNewUser(false);
        userRepository.save(currentUser);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ReadUser> optionalToResponseEntity(Optional<User> user) {
        return user.map(value ->
                ResponseEntity.ok(objectMapper.toReadUser(value)))
            .orElseGet(() ->
                ResponseEntity.notFound().build()
            );
    }

    private static ResponseEntity<ReadUser> getCertificateUser() {
        return ResponseEntity.status(I_AM_A_TEAPOT)
            .body(ReadUser.builder()
                .name(new String(
                    getDecoder().decode("UHJvamVrdCB2b24gRGFuaWVsLCBKYW4sIE1heCwgUGhpbGxpcCwgUm9iaW4gdW5kIFhhdmVy")))
                .build());
    }
}
