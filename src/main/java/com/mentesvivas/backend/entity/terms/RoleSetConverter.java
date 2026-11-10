package com.mentesvivas.backend.entity.terms;

import com.mentesvivas.backend.entity.user.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class RoleSetConverter implements AttributeConverter<Set<Role>, String> {

    @Override
    public String convertToDatabaseColumn(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream().map(Role::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public Set<Role> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return EnumSet.noneOf(Role.class);
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        Arrays.stream(value.split(",")).map(Role::valueOf).forEach(roles::add);
        return roles;
    }
}
