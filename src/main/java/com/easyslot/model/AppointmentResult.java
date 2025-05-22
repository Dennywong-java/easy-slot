package com.easyslot.model;

import lombok.Data;

@Data
public class AppointmentResult {
    private boolean success;
    private String city;
    private String date;
    private String time;
    private boolean autoBooked;
    private String error;
} 