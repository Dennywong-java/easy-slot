package com.easyslot.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * State Management Service
 * Responsible for managing and persisting the state of all workers
 */
@Service
public class StateManager {
    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);
    private static final String STATE_DIR = "state";
    private static final String STATE_FILE_SUFFIX = "_state.json";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    // Map to store all worker states, with email as key
    private final Map<String, State> stateMap = new ConcurrentHashMap<>();
    
    public StateManager() {
        ensureStateDirectory();
    }
    
    /**
     * Get a user's state manager, create if it doesn't exist
     */
    public State getOrCreateState(String email) {
        return stateMap.computeIfAbsent(email, k -> {
            Path statePath = Paths.get(STATE_DIR, generateStateFileName(k));
            State state = loadState(statePath).orElse(new State());
            state.setEmail(email);
            return state;
        });
    }
    
    private String generateStateFileName(String email) {
        try {
            // Use MD5 to generate a fixed-length hash value
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(email.getBytes());
            // Use Base64 encoding and remove unsafe characters
            String safeHash = Base64.getEncoder().encodeToString(hash)
                    .replaceAll("[^a-zA-Z0-9]", "")
                    .substring(0, 16); // Take the first 16 characters to keep the filename short
            return safeHash + STATE_FILE_SUFFIX;
        } catch (NoSuchAlgorithmException e) {
            // If MD5 is not available, use a simple fallback method
            String safeName = email.replaceAll("[^a-zA-Z0-9]", "");
            return safeName.substring(0, Math.min(safeName.length(), 16)) + STATE_FILE_SUFFIX;
        }
    }

    private void ensureStateDirectory() {
        try {
            Files.createDirectories(Paths.get(STATE_DIR));
        } catch (IOException e) {
            logger.error("Failed to create state directory", e);
        }
    }

    private Optional<State> loadState(Path statePath) {
        if (Files.exists(statePath)) {
            try {
                return Optional.of(objectMapper.readValue(statePath.toFile(), State.class));
            } catch (IOException e) {
                logger.error("Failed to load state file", e);
            }
        }
        return Optional.empty();
    }

    public void updateState(String email, String status, String dateRange, String location, 
                          boolean slotAvailable, String notes) {
        State state = getOrCreateState(email);
        state.setStatus(status);
        state.setLastChecked(LocalDateTime.now());
        state.setDateRange(dateRange);
        state.setLocation(location);
        state.setEmail(email);
        if (slotAvailable) {
            state.setLastSlotFound(LocalDateTime.now());
        }
        state.setSlotAvailable(slotAvailable);
        state.setNotes(notes);
        saveState(email);
    }
    
    /**
     * Update login status for a user
     *
     * @param email User email
     * @param loggedIn Whether the user is logged in
     */
    public void updateLoginState(String email, boolean loggedIn) {
        State state = getOrCreateState(email);
        state.setStatus(loggedIn ? "logged_in" : "login_failed");
        state.setLastChecked(LocalDateTime.now());
        state.setNotes(loggedIn ? "Successfully logged in" : "Login failed");
        saveState(email);
        logger.info("Updated login state for {}: {}", email, loggedIn ? "success" : "failed");
    }

    private void saveState(String email) {
        State state = getOrCreateState(email);
        try {
            Path statePath = Paths.get(STATE_DIR, generateStateFileName(email));
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(statePath.toFile(), state);
        } catch (IOException e) {
            logger.error("Failed to save state file for {}", email, e);
        }
    }

    public State getState(String email) {
        return getOrCreateState(email);
    }
    
    /**
     * Get states of all workers
     */
    public Map<String, State> getAllStates() {
        return new ConcurrentHashMap<>(stateMap);
    }

    public static class State {
        private String status = "initializing";
        private LocalDateTime lastChecked;
        private String dateRange = "";
        private String location = "";
        private LocalDateTime lastSlotFound;
        private boolean slotAvailable = false;
        private String notes = "";
        private String email = "";

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getLastChecked() { return lastChecked; }
        public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }
        
        public String getDateRange() { return dateRange; }
        public void setDateRange(String dateRange) { this.dateRange = dateRange; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public LocalDateTime getLastSlotFound() { return lastSlotFound; }
        public void setLastSlotFound(LocalDateTime lastSlotFound) { this.lastSlotFound = lastSlotFound; }
        
        public boolean isSlotAvailable() { return slotAvailable; }
        public void setSlotAvailable(boolean slotAvailable) { this.slotAvailable = slotAvailable; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
} 