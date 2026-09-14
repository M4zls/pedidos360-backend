package com.pedidos360.identity.controller;

import java.util.List;

import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.service.UserDirectory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administracion de roles. Todo el path {@code /api/admin/**} exige rol ADMIN
 * (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserDirectory users;

    public AdminUserController(UserDirectory users) {
        this.users = users;
    }

    @GetMapping
    public List<AppUserResponse> list() {
        return users.list().stream().map(AppUserResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public AppUserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest body) {
        return AppUserResponse.from(users.updateRole(id, body.role()));
    }

    public record UpdateRoleRequest(@NotNull Role role) {}
}
