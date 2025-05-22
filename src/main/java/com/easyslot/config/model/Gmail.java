package com.easyslot.config.model;

import lombok.Data;

@Data
public class Gmail {
    private String email;
    private String appPassword;
    private String recipientEmail;
    private boolean enabled;
} 