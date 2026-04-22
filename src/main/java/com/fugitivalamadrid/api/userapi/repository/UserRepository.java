package com.fugitivalamadrid.api.userapi.repository;

import com.fugitivalamadrid.api.userapi.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Find users by username containing the given string (case-insensitive) with pagination.
     *
     * @param username the username search term
     * @param pageable pagination and sorting parameters
     * @return paginated list of matching users
     */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}