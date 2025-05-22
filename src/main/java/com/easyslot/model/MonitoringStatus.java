package com.easyslot.model;

import lombok.Data;

@Data
public class MonitoringStatus {
    private boolean running;
    private String lastCheckTime;
    private String currentCity;
    private String currentError;
    private String lastFoundAppointment;
} 