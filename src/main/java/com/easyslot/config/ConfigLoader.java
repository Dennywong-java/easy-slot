package com.easyslot.config;

import com.easyslot.config.model.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration file loader
 */
@Component
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String DEFAULT_CONFIG_PATH = "config.yml";
    
    @Value("${config.path:}")
    private String configPath;
    
    /**
     * Load application configuration
     * 
     * @return Application configuration object
     */
    public AppConfig loadConfig() {
        // Determine configuration file path
        String finalPath = configPath != null && !configPath.isEmpty() 
                ? configPath 
                : DEFAULT_CONFIG_PATH;
        
        File configFile = new File(finalPath);
        
        try {
            logger.info("Trying to load configuration from {}", configFile.getAbsolutePath());
            
            if (!configFile.exists()) {
                logger.warn("Configuration file does not exist: {}", configFile.getAbsolutePath());
                
                // If external configuration file doesn't exist, try to load from classpath
                logger.info("Trying to load default configuration from classpath");
                return loadFromClasspath();
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            AppConfig config = mapper.readValue(configFile, AppConfig.class);
            logger.info("Configuration loaded successfully");
            return config;
            
        } catch (Exception e) {
            logger.error("Failed to load configuration file: {}", e.getMessage());
            // Create default configuration instead of throwing exception
            logger.info("Using default configuration");
            return createDefaultConfig();
        }
    }
    
    /**
     * Load configuration from classpath
     */
    private AppConfig loadFromClasspath() {
        try {
            ClassPathResource resource = new ClassPathResource("config.yml");
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            AppConfig config = mapper.readValue(resource.getInputStream(), AppConfig.class);
            logger.info("Successfully loaded configuration from classpath");
            return config;
        } catch (IOException e) {
            logger.warn("Unable to load configuration from classpath: {}", e.getMessage());
            logger.info("Using in-memory default configuration");
            
            // Create a basic default configuration
            return createDefaultConfig();
        }
    }
    
    /**
     * Create default configuration object
     */
    private AppConfig createDefaultConfig() {
        AppConfig config = new AppConfig();
        config.setMonitoring(new AppConfig.MonitoringConfig());
        config.setBrowser(new AppConfig.BrowserConfig());
        config.setNotification(new AppConfig.NotificationConfig());
        return config;
    }
} 