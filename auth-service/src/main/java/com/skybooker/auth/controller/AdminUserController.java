package com.skybooker.auth.controller;

import com.skybooker.auth.dto.MessageResponse;
import com.skybooker.auth.dto.UserResponse;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return authService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable String userId) {
        return authService.getUserById(userId);
    }

    @GetMapping("/role/{role}")
    public List<UserResponse> getUsersByRole(@PathVariable Role role) {
        return authService.getUsersByRole(role);
    }

    @PutMapping("/{userId}/suspend")
    public MessageResponse suspendUser(@PathVariable String userId) {
        return authService.suspendUser(userId);
    }

    @PutMapping("/{userId}/reactivate")
    public MessageResponse reactivateUser(@PathVariable String userId) {
        return authService.reactivateUser(userId);
    }

    @DeleteMapping("/{userId}")
    public MessageResponse deleteUser(@PathVariable String userId) {
        return authService.deleteUser(userId);
    }
}