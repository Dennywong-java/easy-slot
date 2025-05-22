package com.easyslot.config;

import com.easyslot.config.model.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

class ConfigLoaderTest {
    @TempDir
    Path tempDir;
    
    private File configFile;
    private ConfigLoader configLoader;
    private static final String VALID_CONFIG = """
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
                notification:
                  enabled: true
                  emailEnabled: true
                  emailAddress: "test@gmail.com"
            """;

    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("config.yml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(VALID_CONFIG);
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
    void testLoadValidConfig() {
        AppConfig config = configLoader.loadConfig();
        
        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getMonitoring(), "Monitoring should not be null");
        assertNotNull(config.getBrowser(), "Browser should not be null");
        assertNotNull(config.getNotification(), "Notification should not be null");
        assertNotNull(config.getUsers(), "Users should not be null");
        
        // Verify user information
        Map<String, AppConfig.UserConfig> users = config.getUsers();
        assertTrue(users.containsKey("test@example.com"), "Should contain test user");
        AppConfig.UserConfig user = users.get("test@example.com");
        
        assertEquals("test@example.com", user.getEmail());
        assertEquals("testpass", user.getPassword());
        
        // Verify user appointment settings
        assertNotNull(user.getAppointment(), "User appointment should not be null");
        assertEquals("Toronto", user.getAppointment().getLocation());
        assertEquals("2024-03-20", user.getAppointment().getStartDate());
        
        // Verify user notification settings
        assertNotNull(user.getNotification(), "User notification settings should not be null");
        assertEquals("test@gmail.com", user.getNotification().getEmailAddress());
        
        // Verify global settings
        assertEquals(300, config.getMonitoring().getCheckIntervalSeconds());
        assertTrue(config.getBrowser().isHeadless());
    }

    @Test
    void testLoadNonExistentConfig() {
        // Set non-existent configuration file path
        String nonExistentPath = tempDir.resolve("non-existent.yml").toString();
        setConfigPath(configLoader, nonExistentPath);
        
        // Should not throw exception, but use default configuration
        AppConfig config = configLoader.loadConfig();
        assertNotNull(config, "Should return default config");
        assertNotNull(config.getMonitoring(), "Default monitoring config should be created");
    }

    @Test
    void testLoadInvalidConfig() {
        String invalidConfig = """
            monitoring:
              invalid: true
            users:
              invalid@user.com:
                email: ""
            """;
        
        File invalidConfigFile = tempDir.resolve("invalid-config.yml").toFile();
        try (FileWriter writer = new FileWriter(invalidConfigFile)) {
            writer.write(invalidConfig);
        } catch (IOException e) {
            fail("Failed to create invalid config file");
        }

        // Set invalid configuration file path
        setConfigPath(configLoader, invalidConfigFile.getAbsolutePath());
        
        // Due to @JsonIgnoreProperties, it should handle unknown properties
        AppConfig config = configLoader.loadConfig();
        assertNotNull(config, "Should return a config object even with invalid input");
    }
} 