package com.easyslot.config.model;

import lombok.Data;

@Data
public class Monitoring {
    private int checkInterval;
    private int retryInterval;
    
    public int getInterval() {
        return checkInterval;
    }
} 