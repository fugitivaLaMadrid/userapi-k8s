package com.fugitivalamadrid.api.userapi.mapper;

import com.fugitivalamadrid.api.userapi.dto.UserRequest;
import com.fugitivalamadrid.api.userapi.dto.UserResponse;
import com.fugitivalamadrid.api.userapi.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps UserRequest to User entity.
     * createdAt is excluded — set manually in the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRequest request);

    /**
     * Maps User entity to UserResponse.
     */
    UserResponse toResponse(User user);
}