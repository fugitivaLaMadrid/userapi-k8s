package com.fugitivalamadrid.api.userapi.service;

import com.fugitivalamadrid.api.userapi.dto.UserPartialRequest;
import com.fugitivalamadrid.api.userapi.dto.UserRequest;
import com.fugitivalamadrid.api.userapi.dto.UserResponse;
import com.fugitivalamadrid.api.userapi.exception.UserNotFoundException;
import com.fugitivalamadrid.api.userapi.mapper.UserMapper;
import com.fugitivalamadrid.api.userapi.model.User;
import com.fugitivalamadrid.api.userapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
 class UserServiceTest {
    // ── Mocks ─────────────────────────────────────────────────────────────────
    // UserRepository is a MOCK — no real database, no Spring context
    // When we call userRepository.findById(), we tell Mockito what to return
    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserMapper userMapper;

    // UserService is the REAL class we are testing
    // Mockito injects the mock UserRepository into it automatically
    @InjectMocks
    private UserService userService;


    // ── Helper ────────────────────────────────────────────────────────────────
    /**
     * Builds a User object with the given id, username, and email.
     */
    private User buildUser(Long id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Get all users should return empty page when no users exist")
    void getAllUsers_returnsEmptyPage_whenNoUsers() {
        // ARRANGE — tell the mock repository to return an empty page
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> emptyPage = Page.empty(pageable);
        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        // ACT — call the real service method
        Page<UserResponse> result = userService.getAllUsers(pageable);

        // ASSERT — verify the result
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Get all users should return mapped page when users exist")
    void getAllUsers_returnsMappedPage_whenUsersExist() {
        // ARRANGE
        User alice = buildUser(1L, "alice", "alice@example.com");
        User bob = buildUser(2L, "bob", "bob@example.com");
        List<User> users = List.of(alice, bob);
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new org.springframework.data.domain.PageImpl<>(users, pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        UserResponse aliceResponse = UserResponse.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .createdAt(alice.getCreatedAt())
                .build();
        UserResponse bobResponse = UserResponse.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .createdAt(bob.getCreatedAt())
                .build();
        when(userMapper.toResponse(alice)).thenReturn(aliceResponse);
        when(userMapper.toResponse(bob)).thenReturn(bobResponse);

        // ACT
        Page<UserResponse> result = userService.getAllUsers(pageable);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        assertThat(result.getContent().get(1).getUsername()).isEqualTo("bob");
    }

    // ── getUserById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Get user by ID should return user response when user exists")
    void getUserById_returnsUser_whenExists() {
        // ARRANGE — mock returns alice wrapped in Optional
        User alice = buildUser(1L, "alice", "alice@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        UserResponse aliceResponse = UserResponse.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .createdAt(alice.getCreatedAt())
                .build();
        when(userMapper.toResponse(alice)).thenReturn(aliceResponse);

        // ACT
        UserResponse result = userService.getUserById(1L);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get user by ID should throw UserNotFoundException when user does not exist")
    void getUserById_throwsUserNotFoundException_whenNotFound() {
        // ARRANGE — mock returns empty Optional (user does not exist)
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT — verify the exception is thrown
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        verify(userRepository, times(1)).findById(999L);
    }

    // ── createUser ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Create user should save and return user response")
    void createUser_savesAndReturnsUser() {
        // ARRANGE
        UserRequest request = new UserRequest("alice", "alice@example.com");
        User userToCreate = User.builder()
                .username("alice")
                .email("alice@example.com")
                .build();
        User savedUser = buildUser(1L, "alice", "alice@example.com");

        when(userMapper.toEntity(request)).thenReturn(userToCreate);
        // When save() is called with any User object, return savedUser
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .createdAt(savedUser.getCreatedAt())
                .build();
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        // ACT
        UserResponse result = userService.createUser(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getCreatedAt()).isNotNull();

        verify(auditLogService).logCreated(anyString());
        // Verify save was called exactly once
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ── deleteUser ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Delete user should delete user when it exists")
    void deleteUser_deletesUser_whenExists() {
        // ARRANGE — user exists
        when(userRepository.existsById(1L)).thenReturn(true);

        // ACT
        userService.deleteUser(1L);

        // ASSERT — verify deleteById was called
        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Delete user should throw UserNotFoundException when user does not exist")
    void deleteUser_throwsUserNotFoundException_whenNotFound() {
        // ARRANGE — user does not exist
        when(userRepository.existsById(999L)).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        // Verify deleteById was NEVER called
        verify(userRepository, never()).deleteById(any());
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Update user should update fields and save when user exists")
    void updateUser_updatesUser_whenExists() {
        // ARRANGE
        User existing = buildUser(1L, "alice", "alice@example.com");
        UserRequest request = new UserRequest("alice-updated", "alice2@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        // ACT
        userService.updateUser(1L, request);

        // ASSERT — verify the fields were updated and save was called
        assertThat(existing.getUsername()).isEqualTo("alice-updated");
        assertThat(existing.getEmail()).isEqualTo("alice2@example.com");
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Update user should throw UserNotFoundException when user does not exist")
    void updateUser_throwsUserNotFoundException_whenNotFound() {
        // ARRANGE
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        UserRequest request = new UserRequest("alice", "alice@example.com");

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.updateUser(999L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        verify(userRepository, never()).save(any());
    }

    // ── updateUserPartial ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Update user partial should update only username when only username provided")
    void updateUserPartial_updatesOnlyUsername_whenOnlyUsernameProvided() {
        // ARRANGE
        User existing = buildUser(1L, "alice", "alice@example.com");
        UserPartialRequest request = new UserPartialRequest("alice-updated", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        // ACT
        userService.updateUserPartial(1L, request);

        // ASSERT — username changed, email stayed the same
        assertThat(existing.getUsername()).isEqualTo("alice-updated");
        assertThat(existing.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Update user partial should update only email when only email provided")
    void updateUserPartial_updatesOnlyEmail_whenOnlyEmailProvided() {
        // ARRANGE
        User existing = buildUser(1L, "alice", "alice@example.com");
        UserPartialRequest request = new UserPartialRequest(null, "newemail@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        // ACT
        userService.updateUserPartial(1L, request);

        // ASSERT — email changed, username stayed the same
        assertThat(existing.getUsername()).isEqualTo("alice");
        assertThat(existing.getEmail()).isEqualTo("newemail@example.com");
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Update user partial should throw UserNotFoundException when user does not exist")
    void updateUserPartial_throwsUserNotFoundException_whenNotFound() {
        // ARRANGE
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        UserPartialRequest request = new UserPartialRequest("alice", null);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.updateUserPartial(999L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Search users should use repository filtering and return mapped page")
    void searchUsers_returnsMappedPage() {
        User alice = buildUser(1L, "alice", "alice@example.com");
        List<User> users = List.of(alice);
        Pageable pageable = PageRequest.of(0, 20, Sort.by("username").ascending());
        Page<User> userPage = new org.springframework.data.domain.PageImpl<>(users, pageable, 1);
        
        UserResponse aliceResponse = UserResponse.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .createdAt(alice.getCreatedAt())
                .build();

        when(userRepository.findByUsernameContainingIgnoreCase(eq("ali"), any(Pageable.class)))
                .thenReturn(userPage);
        when(userMapper.toResponse(alice)).thenReturn(aliceResponse);

        Page<UserResponse> result = userService.searchUsers("ali", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        verify(userRepository, times(1))
                .findByUsernameContainingIgnoreCase(eq("ali"), any(Pageable.class));
    }

    @Test
    @DisplayName("Search users should return empty page when no matches found")
    void searchUsers_returnsEmptyPage_whenNoMatches() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> emptyPage = Page.empty(pageable);
        
        when(userRepository.findByUsernameContainingIgnoreCase(eq("xyz"), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<UserResponse> result = userService.searchUsers("xyz", pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(userRepository, times(1))
                .findByUsernameContainingIgnoreCase(eq("xyz"), any(Pageable.class));
    }
} 
