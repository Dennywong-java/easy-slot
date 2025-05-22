package com.easyslot.config.model;

import lombok.Data;
import java.util.Map;

@Data
public class Browser {
    private String type;
    private boolean headless;
    private String email;
    private BrowserOptions options;
    
    @Data
    public static class BrowserOptions {
        private Map<String, Object> chrome;
        private Map<String, Object> firefox;
        private Map<String, Object> edge;
    }
} 