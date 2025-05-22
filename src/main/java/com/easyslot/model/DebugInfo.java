package com.easyslot.model;

import lombok.Data;

@Data
public class DebugInfo {
    private String timestamp;
    private String prefix;
    private String screenshotPath;
    private String pageSourcePath;
    private String url;
} 