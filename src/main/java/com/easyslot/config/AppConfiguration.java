package com.easyslot.config;

import com.easyslot.config.model.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Application configuration class
 * Responsible for managing configuration loading
 */
@Configuration
public class AppConfiguration {

    private final ConfigLoader configLoader;
    
    @Autowired
    public AppConfiguration(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }
    
    /**
     * Load application configuration
     */
    @Bean
    public AppConfig appConfig() {
        return configLoader.loadConfig();
    }

    /**
     * Work thread pool configuration
     * Used for basic task processing
     */
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("task-");
        // Rejection policy: caller runs
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
} 