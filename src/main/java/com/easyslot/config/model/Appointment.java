package com.easyslot.config.model;

import lombok.Data;
import java.util.List;

@Data
public class Appointment {
    private String ivrNumber;
    private List<String> preferredCities;
    private DateRange dateRange;
    private String location;
    private boolean autoBook;
} 