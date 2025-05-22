package com.easyslot.service.user;

import com.easyslot.model.dto.UserDTO;

import java.util.List;

/**
 * Service interface for user operations
 */
public interface UserService {
    
    /**
     * Get all users
     * 
     * @return List of all users
     */
    List<UserDTO> getAllUsers();
    
    /**
     * Get a user by email
     * 
     * @param email User email
     * @return User information
     */
    UserDTO getUserByEmail(String email);
    
    /**
     * Add a new user
     * 
     * @param userDTO User information
     * @return Added user
     */
    UserDTO addUser(UserDTO userDTO);
    
    /**
     * Update user information
     * 
     * @param email User email
     * @param userDTO Updated user information
     * @return Updated user
     */
    UserDTO updateUser(String email, UserDTO userDTO);
    
    /**
     * Delete a user
     * 
     * @param email User email
     * @return True if user was deleted
     */
    boolean deleteUser(String email);
    
    /**
     * Authenticate a user
     * 
     * @param email User email
     * @param password User password
     * @return True if authentication was successful
     */
    boolean authenticate(String email, String password);
} 