package com.easyslot.controller;

import com.easyslot.model.dto.UserDTO;
import com.easyslot.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for user operations
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get all users
     * 
     * @return List of all users
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Get user by email
     * 
     * @param email User email
     * @return User information
     */
    @GetMapping("/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    /**
     * Add a new user
     * 
     * @param userDTO User information
     * @return Operation result
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addUser(@RequestBody UserDTO userDTO) {
        userService.addUser(userDTO);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "User added successfully"
        ));
    }

    /**
     * Update user information
     * 
     * @param email User email
     * @param userDTO Updated user information
     * @return Operation result
     */
    @PutMapping("/{email}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String email,
            @RequestBody UserDTO userDTO) {
        userService.updateUser(email, userDTO);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "User updated successfully"
        ));
    }

    /**
     * Delete a user
     * 
     * @param email User email
     * @return Operation result
     */
    @DeleteMapping("/{email}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String email) {
        userService.deleteUser(email);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "User deleted successfully"
        ));
    }
} 