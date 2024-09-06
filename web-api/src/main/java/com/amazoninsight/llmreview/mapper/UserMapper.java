package com.amazoninsight.llmreview.mapper;

import com.amazoninsight.llmreview.dto.RegistrationDTO;
import com.amazoninsight.llmreview.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting between RegistrationDTO and User entities.
 * Utilizes MapStruct for automatic mapping.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Converts a RegistrationDTO to a User entity.
     *
     * @param registrationDTO the registration data transfer object
     * @return the corresponding User entity
     */
    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "password", target = "password")
    User registrationDtoToUser(RegistrationDTO registrationDTO);
}