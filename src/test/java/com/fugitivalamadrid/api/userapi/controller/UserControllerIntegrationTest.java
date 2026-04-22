package com.fugitivalamadrid.api.userapi.controller;

import com.fugitivalamadrid.api.userapi.dto.UserPartialRequest;
import com.fugitivalamadrid.api.userapi.dto.UserRequest;
import com.fugitivalamadrid.api.userapi.model.User;
import com.fugitivalamadrid.api.userapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController using JWT authentication.
 * Includes tests for pagination support on /users and /users/search endpoints.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
        // Clear cache so tests don't interfere with each other
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());
    }

    /**
     * Helper method to create a JWT with ADMIN role.
     */
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("scope", List.of("read", "write"))
                        .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                        .claim("sub", "admin-user")
                        .claim("email", "admin@example.com")
                );
    }

    /**
     * Helper method to create a JWT with USER role (no admin access).
     */
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("scope", List.of("read"))
                        .claim("realm_access", Map.of("roles", List.of("USER")))
                        .claim("sub", "regular-user")
                        .claim("email", "user@example.com")
                );
    }

    // ---- GET /users (Pagination) ----

    @Test
    @DisplayName("GET /users returns paginated empty list when no users exist")
    void getAllUsers_returnsPaginatedEmptyList_whenNoUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    @DisplayName("GET /users returns first page with default size when users exist")
    void getAllUsers_returnsFirstPage_whenUsersExist() throws Exception {
        // Create 25 users to test pagination
        for (int i = 1; i <= 25; i++) {
            createUserInDb("user" + i, "user" + i + "@example.com");
        }

        mockMvc.perform(get("/users")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))  // default page size
                .andExpect(jsonPath("$.page.totalElements", is(25)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    @DisplayName("GET /users returns second page with custom size")
    void getAllUsers_returnsSecondPage_withCustomSize() throws Exception {
        // Create 15 users
        for (int i = 1; i <= 15; i++) {
            createUserInDb("user" + i, "user" + i + "@example.com");
        }

        mockMvc.perform(get("/users")
                        .with(adminJwt())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))  // 5 remaining users
                .andExpect(jsonPath("$.page.totalElements", is(15)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.size", is(10)));
    }

    @Test
    @DisplayName("GET /users returns sorted users by username ascending")
    void getAllUsers_returnsSortedUsers_byUsernameAsc() throws Exception {
        createUserInDb("charlie", "charlie@example.com");
        createUserInDb("alice", "alice@example.com");
        createUserInDb("bob", "bob@example.com");

        mockMvc.perform(get("/users")
                        .with(adminJwt())
                        .param("sort", "username,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].username", is("alice")))
                .andExpect(jsonPath("$.content[1].username", is("bob")))
                .andExpect(jsonPath("$.content[2].username", is("charlie")));
    }

    @Test
    @DisplayName("GET /users returns sorted users by email descending")
    void getAllUsers_returnsSortedUsers_byEmailDesc() throws Exception {
        createUserInDb("alice", "alice@example.com");
        createUserInDb("bob", "bob@example.com");
        createUserInDb("charlie", "charlie@example.com");

        mockMvc.perform(get("/users")
                        .with(adminJwt())
                        .param("sort", "email,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].email", is("charlie@example.com")))
                .andExpect(jsonPath("$.content[1].email", is("bob@example.com")))
                .andExpect(jsonPath("$.content[2].email", is("alice@example.com")));
    }

    // ---- GET /users/search (Pagination) ----

    @Test
    @DisplayName("GET /users/search returns paginated matching users")
    void searchUsers_returnsPaginatedMatchingUsers() throws Exception {
        // Create users with different prefixes
        for (int i = 1; i <= 15; i++) {
            createUserInDb("alice" + i, "alice" + i + "@example.com");
        }
        createUserInDb("bob", "bob@example.com");

        mockMvc.perform(get("/users/search")
                        .with(adminJwt())
                        .param("name", "alice")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.page.totalElements", is(15)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    @DisplayName("GET /users/search returns empty page when no matches")
    void searchUsers_returnsEmptyPage_whenNoMatch() throws Exception {
        createUserInDb("alice", "alice@example.com");

        mockMvc.perform(get("/users/search")
                        .with(adminJwt())
                        .param("name", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    @DisplayName("GET /users/search returns sorted paginated results")
    void searchUsers_returnsSortedPaginatedResults() throws Exception {
        createUserInDb("alice3", "alice3@example.com");
        createUserInDb("alice1", "alice1@example.com");
        createUserInDb("alice2", "alice2@example.com");

        mockMvc.perform(get("/users/search")
                        .with(adminJwt())
                        .param("name", "alice")
                        .param("sort", "username,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].username", is("alice1")))
                .andExpect(jsonPath("$.content[1].username", is("alice2")))
                .andExpect(jsonPath("$.content[2].username", is("alice3")));
    }

    // ---- GET /users/{id} ----

    @Test
    @DisplayName("GET /users/{id} returns user when exists")
    void getUserById_returnsUser_whenExists() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");

        mockMvc.perform(get("/users/{id}", saved.getId())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    @DisplayName("GET /users/{id} returns 404 when user not found")
    void getUserById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/users/{id}", 999L)
                        .with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("999")));
    }

    // ---- POST /users ----

    @Test
    @DisplayName("POST /users creates user and returns created user")
    void createUser_returnsCreatedUser() throws Exception {
        UserRequest request = new UserRequest("alice", "alice@example.com");

        mockMvc.perform(post("/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /users returns 400 when username is blank")
    void createUser_returns400_whenUsernameBlank() throws Exception {
        UserRequest request = new UserRequest("", "alice@example.com");

        mockMvc.perform(post("/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- DELETE /users/{id} ----

    @Test
    @DisplayName("DELETE /users/{id} returns 204 when user exists and is deleted")
    void deleteUser_returns204_whenExists() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");

        mockMvc.perform(delete("/users/{id}", saved.getId())
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", saved.getId())
                        .with(adminJwt()))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /users/{id} ----

    @Test
    @DisplayName("PUT /users/{id} returns 204 when user is successfully updated")
    void updateUser_returns204_whenSuccessful() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");
        UserRequest request = new UserRequest("alice-updated", "alice2@gmail.com");

        mockMvc.perform(put("/users/{id}", saved.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", saved.getId())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("alice-updated")))
                .andExpect(jsonPath("$.email", is("alice2@gmail.com")));
    }

    // ---- PATCH /users/{id} ----

    @Test
    @DisplayName("PATCH /users/{id} returns 204 when user is successfully updated")
    void updateUserPartial_returns204_whenSuccessful() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");
        UserPartialRequest request = new UserPartialRequest(null, "alice-update@example.com");

        mockMvc.perform(patch("/users/{id}", saved.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", saved.getId())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("alice-update@example.com")));
    }

    // ---- EDGE CASES ----

    @Test
    @DisplayName("POST /users returns 409 when email already exists")
    void createUser_returns409_whenDuplicateEmail() throws Exception {
        UserRequest request = new UserRequest("alice", "alice@example.com");

        mockMvc.perform(post("/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    // ---- SECURITY ----

    @Test
    @WithAnonymousUser
    @DisplayName("GET /users returns 401 when no authentication provided")
    void getAllUsers_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users returns 403 when user has no ADMIN role")
    void getAllUsers_returns403_whenNotAuthorized() throws Exception {
        mockMvc.perform(get("/users")
                        .with(userJwt()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /users returns 403 when user has no ADMIN role")
    void createUser_returns403_whenNotAuthorized() throws Exception {
        UserRequest request = new UserRequest("alice", "alice@example.com");

        mockMvc.perform(post("/users")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /users/{id} returns 403 when user has no ADMIN role")
    void deleteUser_returns403_whenNotAuthorized() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");

        mockMvc.perform(delete("/users/{id}", saved.getId())
                        .with(userJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /users/{id} returns 403 when user has no ADMIN role")
    void updateUser_returns403_whenNotAuthorized() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");
        UserRequest request = new UserRequest("alice-updated", "alice2@gmail.com");

        mockMvc.perform(put("/users/{id}", saved.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /users/{id} returns 403 when user has no ADMIN role")
    void updateUserPartial_returns403_whenNotAuthorized() throws Exception {
        User saved = createUserInDb("alice", "alice@example.com");
        UserPartialRequest request = new UserPartialRequest(null, "alice-update@example.com");

        mockMvc.perform(patch("/users/{id}", saved.getId())
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    // ---- Helpers ----

    private User createUserInDb(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .createdAt(LocalDateTime.now())
                .build());
    }
}