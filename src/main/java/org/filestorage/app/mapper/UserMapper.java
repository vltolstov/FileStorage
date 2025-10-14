package org.filestorage.app.mapper;

import org.filestorage.app.dto.UserRequest;
import org.filestorage.app.dto.UserResponse;
import org.filestorage.app.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "username", target = "name")
    User toEntity(UserRequest userRequest);

    @Mapping(source = "name", target = "username")
    UserResponse toResponse(User user);

}
