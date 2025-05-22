package com.easyslot.service.user;

import com.easyslot.model.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;

/**
 * User service implementation
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    // In-memory user data storage, can be replaced with database in the future
    private final Map<String, UserDTO> users = new ConcurrentHashMap<>();
    
    public UserServiceImpl() {
        // Add test user
        createTestUser();
    }
    
    /**
     * Simple password encoding, only for development testing
     */
    private String encodePassword(String rawPassword) {
        // Simple Base64 encoding, only for development environment
        return Base64.getEncoder().encodeToString(rawPassword.getBytes());
    }
    
    /**
     * Verify password
     */
    private boolean checkPassword(String rawPassword, String encodedPassword) {
        String encoded = encodePassword(rawPassword);
        return encoded.equals(encodedPassword);
    }
    
    private void createTestUser() {
        UserDTO testUser = new UserDTO();
        testUser.setEmail("test@example.com");
        testUser.setPassword(encodePassword("password"));
        testUser.setName("Test User");
        
        // Initialize notification settings
        UserDTO.NotificationSettingDTO notificationSettings = new UserDTO.NotificationSettingDTO();
        notificationSettings.setEmailEnabled(true);
        notificationSettings.setEmailAddress("test@example.com");
        notificationSettings.setEmailPassword("app-password");
        testUser.setNotificationSettings(notificationSettings);
        
        // Initialize browser settings
        UserDTO.BrowserSettingDTO browserSettings = new UserDTO.BrowserSettingDTO();
        browserSettings.setBrowserType("chrome");
        browserSettings.setHeadless(true);
        testUser.setBrowserSettings(browserSettings);
        
        users.put(testUser.getEmail(), testUser);
        logger.info("Created test user: {}", testUser.getEmail());
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        UserDTO user = users.get(email);
        if (user == null) {
            logger.warn("User not found: {}", email);
        }
        return user;
    }

    @Override
    public UserDTO addUser(UserDTO userDTO) {
        if (users.containsKey(userDTO.getEmail())) {
            logger.warn("User already exists: {}", userDTO.getEmail());
            throw new IllegalArgumentException("User already exists");
        }
        
        // Encrypt password
        userDTO.setPassword(encodePassword(userDTO.getPassword()));
        users.put(userDTO.getEmail(), userDTO);
        logger.info("Added new user: {}", userDTO.getEmail());
        return userDTO;
    }

    @Override
    public UserDTO updateUser(String email, UserDTO userDTO) {
        if (!users.containsKey(email)) {
            logger.warn("User to update does not exist: {}", email);
            throw new IllegalArgumentException("User does not exist");
        }
        
        UserDTO existingUser = users.get(email);
        
        // Update user information but keep original password (unless explicitly requested to change)
        if (userDTO.getName() != null) {
            existingUser.setName(userDTO.getName());
        }
        
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(encodePassword(userDTO.getPassword()));
        }
        
        if (userDTO.getNotificationSettings() != null) {
            existingUser.setNotificationSettings(userDTO.getNotificationSettings());
        }
        
        if (userDTO.getBrowserSettings() != null) {
            existingUser.setBrowserSettings(userDTO.getBrowserSettings());
        }
        
        // If changing email, need to remove old key and add new key
        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(email)) {
            users.remove(email);
            users.put(userDTO.getEmail(), existingUser);
            logger.info("User email changed from {} to {}", email, userDTO.getEmail());
        }
        
        logger.info("Updated user: {}", email);
        return existingUser;
    }

    @Override
    public boolean deleteUser(String email) {
        if (users.remove(email) != null) {
            logger.info("Deleted user: {}", email);
            return true;
        } else {
            logger.warn("Attempted to delete non-existent user: {}", email);
            return false;
        }
    }

    @Override
    public boolean authenticate(String email, String password) {
        UserDTO user = users.get(email);
        if (user == null) {
            logger.warn("Authentication failed, user does not exist: {}", email);
            return false;
        }
        
        boolean authenticated = checkPassword(password, user.getPassword());
        if (authenticated) {
            logger.info("User authentication successful: {}", email);
        } else {
            logger.warn("User authentication failed, incorrect password: {}", email);
        }
        
        return authenticated;
    }
} 