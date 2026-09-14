package com.pedidos360.identity.controller;

import java.time.Instant;

import com.pedidos360.identity.domain.AppUser;
import com.pedidos360.identity.domain.Role;

public record AppUserResponse(
        Long id,
        String email,
        String name,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
    public static AppUserResponse from(AppUser u) {
        return new AppUserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole(),
                u.getCreatedAt(), u.getUpdatedAt());
    }
}
