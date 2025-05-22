package com.easyslot.scheduler;

import com.easyslot.config.model.AppConfig;
import com.easyslot.config.ConfigLoader;
import com.easyslot.service.notification.NotificationServiceInterface;
import com.easyslot.state.StateManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class AppointmentSchedulerTest {
    @TempDir
    Path tempDir;
    
    private File configFile;
    private ConfigLoader configLoader;
    private static final String TEST_CONFIG = """
            monitoring:
              checkIntervalSeconds: 300
              errorRetryIntervalSeconds: 60
              maxRetries: 3
            browser:
              type: "chrome"
              headless: true
            notification:
              enabled: true
              emailEnabled: true
              smsEnabled: false
              smtpHost: "smtp.gmail.com"
              smtpPort: 587
            users:
              test@example.com:
                email: "test@example.com"
                password: "testpass"
                appointment:
                  location: "Toronto"
                  startDate: "2024-03-20"
                  endDate: "2024-12-31"
                  preferredCities:
                    - "Toronto"
                    - "Vancouver"
                  autoBook: false
            """;

    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("test-config.yml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(TEST_CONFIG);
        }
        
        // Create ConfigLoader instance
        configLoader = new ConfigLoader();
        // Inject configuration file path
        setConfigPath(configLoader, configFile.getAbsolutePath());
    }
    
    /**
     * Use reflection to set configPath property of ConfigLoader
     */
    private void setConfigPath(ConfigLoader loader, String path) {
        try {
            java.lang.reflect.Field field = ConfigLoader.class.getDeclaredField("configPath");
            field.setAccessible(true);
            field.set(loader, path);
        } catch (Exception e) {
            fail("Failed to set configPath: " + e.getMessage());
        }
    }

    @Test
    void testSchedulerCreation() {
        assertDoesNotThrow(() -> {
            // Load configuration
            AppConfig config = configLoader.loadConfig();
            
            // Initialize state manager
            StateManager stateManager = new StateManager();
            
            // Get test user email
            String email = "test@example.com";
            
            // Create user state
            stateManager.getOrCreateState(email);

            // Mock notification service for testing
            NotificationServiceInterface notificationService = mock(NotificationServiceInterface.class);

            // Create scheduler (now using single user mode)
            AppointmentScheduler scheduler = new AppointmentScheduler(config, stateManager, notificationService);
            
            // No longer need setActiveUser, as it's now single user mode
            // The first user is automatically loaded from config in init() method
            
            // Clean up resources
            scheduler.close();
        });
    }

    @Test
    void testSchedulerWithEmptyConfig() {
        // Create empty configuration
        String emptyConfig = """
                monitoring:
                  checkIntervalSeconds: 30
                """;
        
        File emptyConfigFile = tempDir.resolve("empty-config.yml").toFile();
        try (FileWriter writer = new FileWriter(emptyConfigFile)) {
            writer.write(emptyConfig);
        } catch (IOException e) {
            fail("Failed to create empty config file");
        }
        
        // Set configuration file path
        setConfigPath(configLoader, emptyConfigFile.getAbsolutePath());
        
        assertDoesNotThrow(() -> {
            // Load empty configuration
            AppConfig config = configLoader.loadConfig();
            
            // Add test user
            if (config.getUsers() == null) {
                Map<String, AppConfig.UserConfig> users = new HashMap<>();
                AppConfig.UserConfig user = new AppConfig.UserConfig();
                user.setEmail("test@example.com");
                user.setPassword("password");
                users.put("test@example.com", user);
                config.setUsers(users);
            }
            
            StateManager stateManager = new StateManager();
            NotificationServiceInterface notificationService = mock(NotificationServiceInterface.class);
            
            // Should not throw exception
            AppointmentScheduler scheduler = new AppointmentScheduler(config, stateManager, notificationService);
            scheduler.close();
        });
    }
} 