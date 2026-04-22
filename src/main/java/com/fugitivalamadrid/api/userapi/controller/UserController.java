package com.fugitivalamadrid.api.userapi.controller;

import com.fugitivalamadrid.api.userapi.dto.UserPartialRequest;
import com.fugitivalamadrid.api.userapi.dto.UserRequest;
import com.fugitivalamadrid.api.userapi.dto.UserResponse;
import com.fugitivalamadrid.api.userapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.fugitivalamadrid.api.userapi.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Users", description = "User management endpoints")
@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "UserService is a Spring-managed bean")
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns a paginated list of all users.
     * Supports pagination, sorting, and filtering.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a paginated list of users
     */
    @Operation(summary = "Get all users with pagination",
            description = "Retrieve users with pagination support. Default page size is 20. " +
                    "Example: /users?page=0&size=10&sort=username,asc")
    @RateLimit
    @GetMapping
    public Page<UserResponse> getAllUsers(
            @Parameter(description = "Pagination parameters (page number, size, sort)",
                    example = "page=0&size=20&sort=username,asc")
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.info("GET /users - fetching users with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return userService.getAllUsers(pageable);
    }

    /**
     * Returns a user by id.
     * @param id the user id
     * @return the user
     */
    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        log.info("GET /users/{} - fetching user by id", id);
        return userService.getUserById(id);
    }

    /**
     * Creates a new user.
     * @param request the user request
     * @return the created user
     */
    @Operation(summary = "Create a new user")
    @RateLimit(maxRequests = 5, windowSizeMillis = 80000)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        log.info("POST /users - creating user with username: {}", request.getUsername());
        return userService.createUser(request);
    }

    /**
     * Deletes a user by id.
     * @param id the user id
     */
    @Operation(summary = "Delete a user by ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        log.info("DELETE /users/{} - deleting user by id", id);
        userService.deleteUser(id);
    }

    /**
     * Updates a user by id.
     * @param id the user id
     * @param request the user request
     */
    @Operation(summary = "Update a user by ID")
    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        log.info("PUT /users/{} - updating user with username: {}", id, request.getUsername());
        userService.updateUser(id, request);
    }

    /**
     * Partially updates a user by id.
     * @param id the user id
     * @param request the user partial request
     */
    @Operation(summary = "Partially update a user by ID")
    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUserPartial(@PathVariable Long id, @Valid @RequestBody UserPartialRequest request) {
        log.info("PATCH /users/{} - partially updating user", id);
        userService.updateUserPartial(id, request);
    }

    /**
     * Searches users by username with optional sorting and pagination.
     *
     * @param name the username to search for
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of matching users
     */
    @Operation(summary = "Search users by username with pagination",
            description = "Search users with pagination support. Default page size is 20. " +
                    "Example: /users/search?name=alice&page=0&size=10&sort=email,desc")
    @GetMapping("/search")
    public Page<UserResponse> searchUsers(
            @Parameter(description = "Username search term", required = true)
            @RequestParam String name,
            @Parameter(description = "Pagination parameters (page number, size, sort)",
                    example = "page=0&size=20&sort=username,asc")
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        log.info("GET /users/search - searching users with name: {}, page={}, size={}",
                name, pageable.getPageNumber(), pageable.getPageSize());
        return userService.searchUsers(name, pageable);
    }
}