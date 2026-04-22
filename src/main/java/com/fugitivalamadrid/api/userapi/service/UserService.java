package com.fugitivalamadrid.api.userapi.service;

import com.fugitivalamadrid.api.userapi.dto.UserPartialRequest;
import com.fugitivalamadrid.api.userapi.dto.UserRequest;
import com.fugitivalamadrid.api.userapi.dto.UserResponse;
import com.fugitivalamadrid.api.userapi.exception.UserNotFoundException;
import com.fugitivalamadrid.api.userapi.mapper.UserMapper;
import com.fugitivalamadrid.api.userapi.model.User;
import com.fugitivalamadrid.api.userapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, AuditLogService auditLogService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.userMapper = userMapper;
    }

    /**
     * Returns a paginated list of all users.
     *
     * @param pageable pagination parameters
     * @return paginated list of users
     */
    @Cacheable(value = "users", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching users from database with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    /**
     * Returns a user by id.
     * @param id the user id
     * @return the user
     */
    @Cacheable(value="users", key="#id")
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new UserNotFoundException(id);
                });
        return userMapper.toResponse(user);
    }

    /**
     * Creates a new user.
     * @param request the user request
     * @return the created user
     */
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse createUser(UserRequest request) {
        User user = userMapper.toEntity(request);
        user.setCreatedAt(LocalDateTime.now());
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        auditLogService.logCreated(request.getUsername());
        log.info("User created with id: {}", response.getId());
        return response;
    }

    /**
     * Deletes a user by id.
     * @param id the user id
     */
    @CacheEvict(value="users", allEntries=true)
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            log.warn("Delete failed - user not found with id: {}", id);
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        auditLogService.logDeleted(id);
        log.info("User deleted with id: {}", id);
    }

    /**
     * Updates a user by id.
     * @param id the user id
     * @param request the user request
     */
    @CacheEvict(value = "users", allEntries = true)
    public void updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed - user not found with id: {}", id);
                    return new UserNotFoundException(id);
                });
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        userRepository.save(user);
        auditLogService.logUpdated(id, request.getUsername());
        log.info("User updated with id: {}", id);
    }

    /**
     * Partially updates a user by id.
     * @param id the user id
     * @param request the user partial request
     */
    @CacheEvict(value="users", allEntries=true)
    public void updateUserPartial(Long id, UserPartialRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Partial update failed - user not found with id: {}", id);
                    return new UserNotFoundException(id);
                });
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        userRepository.save(user);
        auditLogService.logPartialUpdated(id);
        log.info("User partially updated with id: {}", id);
    }

    /**
     * Searches users by username with pagination and sorting.
     *
     * @param name the username to search for
     * @param pageable pagination and sorting parameters
     * @return paginated list of matching users
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String name, Pageable pageable) {
        log.info("Searching users with name: {}, page={}, size={}",
                name, pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findByUsernameContainingIgnoreCase(name, pageable)
                .map(userMapper::toResponse);
    }
}