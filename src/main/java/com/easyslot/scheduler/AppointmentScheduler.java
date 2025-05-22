package com.easyslot.scheduler;

import com.easyslot.browser.BrowserFactory;
import com.easyslot.config.model.*;
import com.easyslot.model.MonitoringStatus;
import com.easyslot.model.AppointmentResult;
import com.easyslot.model.DebugInfo;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.easyslot.service.notification.NotificationServiceInterface;
import com.easyslot.state.StateManager;

/**
 * Appointment Scheduler Service
 * Manages browser automation, login and appointment scheduling logic
 * Simplified version: single user mode, no asynchronous processing
 */
@Service
public class AppointmentScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentScheduler.class);
    private static final String BASE_URL = "https://ais.usvisa-info.com/en-ca/niv";
    // Minimum email notification interval (milliseconds)
    private static final long EMAIL_NOTIFICATION_INTERVAL = 5 * 60 * 1000; // 5 minutes

    private WebDriver driver;
    private final AppConfig config;
    private Path logsDir;
    private MonitoringStatus status;
    private final StateManager stateManager;
    private final NotificationServiceInterface notificationService;
    private boolean isRunning = false;

    // Last email notification time
    private long lastEmailNotificationTime = 0;
    
    // User configuration
    private String userEmail;
    private String userPassword;
    private AppConfig.AppointmentConfig appointmentConfig;

    /**
     * Constructor, receives Spring injected dependencies
     */
    @Autowired
    public AppointmentScheduler(AppConfig config, StateManager stateManager, NotificationServiceInterface notificationService) {
        this.config = config;
        this.stateManager = stateManager;
        this.notificationService = notificationService;
        
        // Initialize
        init();
    }
    
    /**
     * Initialize method, sets browser, notification, and logging
     */
    private void init() {
        // Load user information from configuration
        loadUserConfig();
        
        // Initialize log directory
        this.logsDir = Paths.get("logs");
        
        // Initialize monitoring status
        this.status = new MonitoringStatus();
        this.status.setRunning(false);
        this.status.setLastCheckTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        setupLogging();
        cleanupOldFiles();
        
        // Register shutdown hook to update state when program terminates
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning) {
                    logger.info("Application shutting down, updating state...");
                    updateStateToStopped("System was shut down");
                    
                    // Close browser if open
                    if (driver != null) {
                        try {
                            BrowserFactory.closeDriver("default");
                            logger.info("Browser closed during shutdown");
                        } catch (Exception e) {
                            logger.error("Error closing browser during shutdown: {}", e.getMessage());
                        }
                    }
                    
                    isRunning = false;
                }
            } catch (Exception e) {
                logger.error("Error in shutdown hook: {}", e.getMessage());
            }
        }));
        logger.info("Shutdown hook registered");
    }

    /**
     * Load user information from configuration
     */
    private void loadUserConfig() {
        if (config.getUsers() != null && !config.getUsers().isEmpty()) {
            // Use the first user
            String email = config.getUsers().keySet().iterator().next();
            AppConfig.UserConfig userConfig = config.getUsers().get(email);
            
            this.userEmail = email;
            this.userPassword = userConfig.getPassword();
            this.appointmentConfig = userConfig.getAppointment();
            
            logger.info("User configuration loaded: {}", email);
        } else {
            logger.warn("No user information in configuration");
        }
    }

    /**
     * Set log directory and configuration
     */
    private void setupLogging() {
        try {
            Files.createDirectories(logsDir);
            logger.info("Log initialization completed");
        } catch (IOException e) {
            logger.error("Failed to create log directory: {}", e.getMessage());
            throw new RuntimeException("Initialization log failed", e);
        }
    }

    /**
     * Clean up old screenshot and log files
     */
    private void cleanupOldFiles() {
        try {
            String[] patterns = {
                    "*.png",
                    "*.log",
                    "*.html",
            };

            for (String pattern : patterns) {
                try {
                    Files.newDirectoryStream(logsDir, pattern).forEach(file -> {
                        try {
                            Files.delete(file);
                            logger.info("Deleted old file: {}", file);
                        } catch (IOException e) {
                            logger.warn("Failed to delete file {} : {}", file, e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    logger.warn("Processing pattern {} failed: {}", pattern, e.getMessage());
                }
            }

            logger.info("Starting new session");
        } catch (Exception e) {
            logger.warn("Error during cleanup: {}", e.getMessage());
        }
    }

    /**
     * Handle important information page
     */
    private void handleImportantInfoPage() {
        try {
            logger.info("Checking important information page...");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            
            // Try to find and click the down arrow
            try {
                wait.until(ExpectedConditions.elementToBeClickable(By.className("down-arrow"))).click();
                logger.info("Clicked down arrow on important information page");
            } catch (TimeoutException e) {
                // If down arrow is not found, try to find title, check if we've already passed the information page
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("header")));
                    logger.info("Already on main page");
                } catch (TimeoutException e2) {
                    logger.info("Important information page not found");
                }
            }

            // Wait for login form to appear
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sign_in_form")));
            
        } catch (Exception e) {
            logger.error("Failed to handle important information page: {}", e.getMessage());
            throw new RuntimeException("Failed to handle important information page", e);
        }
    }

    /**
     * Handle login form submission
     */
    private void handleLoginForm() {
        logger.info("Finding login form...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sign_in_form")));
        logger.info("Found login form");

        // Fill email
        logger.info("Filling email field...");
        var emailField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("user_email")));
        emailField.clear();
        emailField.sendKeys(userEmail);
        logger.info("Email input successful");

        // Fill password
        logger.info("Filling password field...");
        var passwordField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("user_password")));
        passwordField.clear();
        passwordField.sendKeys(userPassword);
        logger.info("Password input successful");

        // Handle privacy policy checkbox
        handlePrivacyCheckbox();

        // Click login button
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.button.primary[name='commit']"))).click();
        logger.info("Clicked login button");
    }

    /**
     * Handle privacy policy checkbox selection
     */
    private void handlePrivacyCheckbox() {
        try {
            logger.info("Handling privacy policy checkbox...");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            
            // Check if privacy checkbox exists and is not selected
            WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("policy_confirmed")));

            if (!checkbox.isSelected()) {
                // Use JavaScript executor to click element, avoiding being obscured by other elements
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkbox);
                // Wait for scrolling to complete
                Thread.sleep(500);
                // Try using JavaScript to click
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
                logger.info("Privacy policy checkbox selected");
            } else {
                logger.info("Privacy policy checkbox already selected");
            }

        } catch (TimeoutException e) {
            logger.info("Privacy policy checkbox not found, possibly not needed");
        } catch (Exception e) {
            logger.error("Error handling privacy checkbox: {}", e.getMessage());
        }
    }

    /**
     * Execute login sequence
     */
    public void login() {
        if (driver == null) {
            // Initialize browser
            com.easyslot.config.model.Browser browser = new com.easyslot.config.model.Browser();
            browser.setType(config.getBrowser().getType());
            browser.setHeadless(config.getBrowser().isHeadless());
            browser.setEmail("default");
            driver = BrowserFactory.createDriver(browser);
            logger.info("Browser initialized");
        }

        if (userEmail == null || userPassword == null) {
            logger.error("User credentials missing, cannot execute login");
            return;
        }

        try {
            logger.info("Navigating to login page...");
            driver.get(BASE_URL + "/users/sign_in");
            logger.info("Page loaded: {}", driver.getTitle());

            // Handle possible "important information" overlay
            handleImportantInfoPage();

            // Handle login form
            handleLoginForm();

            // Wait for successful login
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.urlContains("/groups/"));

            logger.info("Login successful");
            stateManager.updateLoginState(userEmail, true);

        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            saveDebugInfo("login_error");
            stateManager.updateLoginState(userEmail, false);
            throw new RuntimeException("Login failed", e);
        }
    }

    /**
     * Close and clean up resources
     */
    public void close() {
        // Update state before closing
        updateStateToStopped("System was manually stopped");
        
        if (driver != null) {
            // Close browser instance
            BrowserFactory.closeDriver("default");
            driver = null;
        }
        
        isRunning = false;
    }

    /**
     * Update state to stopped
     * 
     * @param message Message to include in the state update
     */
    private void updateStateToStopped(String message) {
        try {
            logger.info("Updating state to stopped: {}", message);
            String dateRange = appointmentConfig != null ? 
                String.format("%s ~ %s", appointmentConfig.getStartDate(), appointmentConfig.getEndDate()) : "N/A";
            String location = appointmentConfig != null ? appointmentConfig.getLocation() : "N/A";
            
            stateManager.updateState(
                userEmail,
                "stopped",
                dateRange,
                location,
                false,
                message
            );
            logger.info("State updated to stopped");
        } catch (Exception e) {
            logger.error("Error updating state to stopped: {}", e.getMessage());
        }
    }

    /**
     * Save screenshot and page source for debugging, or just print log
     *
     * @param prefix Save file prefix
     */
    private void saveDebugInfo(String prefix) {
        try {
            if (driver == null) {
                logger.warn("Driver is null, cannot save debug info");
                return;
            }

            // Check if debug mode is enabled
            boolean debugEnabled = config.getDebug() != null && config.getDebug().isEnabled();
            String currentUrl = driver.getCurrentUrl();

            // If debug mode is not enabled, only log to console, don't save any files
            if (!debugEnabled) {
                boolean isErrorPrefix = prefix.toLowerCase().contains("error");
                String logLevel = isErrorPrefix ? "ERROR" : "INFO";
                String message = String.format("%s: %s - Current URL: %s", 
                    isErrorPrefix ? "Error occurred" : "Debug info", 
                    prefix, 
                    currentUrl);
                
                if (isErrorPrefix) {
                    logger.error(message);
                } else {
                    logger.info(message);
                }
                return;
            }

            // Create timestamp for unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path screenshotPath = null;
            Path pageSourcePath = null;
            
            // Save screenshot (if configuration allows)
            if (config.getDebug() == null || config.getDebug().isSaveScreenshots()) {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                screenshotPath = logsDir.resolve(prefix + "_" + timestamp + ".png");
                Files.copy(screenshot.toPath(), screenshotPath);
                logger.info("Screenshot saved: {}", screenshotPath);
            }
            
            // Save page source (if configuration allows)
            if (config.getDebug() == null || config.getDebug().isSaveHtml()) {
                String pageSource = driver.getPageSource();
                pageSourcePath = logsDir.resolve(prefix + "_" + timestamp + ".html");
                Files.writeString(pageSourcePath, pageSource);
                logger.info("HTML source saved: {}", pageSourcePath);
            }
            
            // Update debug info
            DebugInfo debugInfo = new DebugInfo();
            if (screenshotPath != null) debugInfo.setScreenshotPath(screenshotPath.toString());
            if (pageSourcePath != null) debugInfo.setPageSourcePath(pageSourcePath.toString());
            debugInfo.setTimestamp(timestamp);
            debugInfo.setUrl(currentUrl);
            
            // Send email notification (if configuration allows and time interval condition is met)
            boolean shouldSendEmail = config.getDebug().isSendEmailNotifications();
            
            if (shouldSendEmail) {
                long currentTime = System.currentTimeMillis();
                long emailInterval = config.getDebug().getEmailIntervalSeconds() > 0 
                    ? config.getDebug().getEmailIntervalSeconds() * 1000 
                    : EMAIL_NOTIFICATION_INTERVAL;
                    
                if (currentTime - lastEmailNotificationTime >= emailInterval) {
                    lastEmailNotificationTime = currentTime;
                    
                    StringBuilder content = new StringBuilder();
                    content.append("EasySlot Debug Info (").append(prefix).append(")\n\n");
                    content.append("Time: ").append(timestamp).append("\n");
                    content.append("Current URL: ").append(currentUrl).append("\n");
                    if (screenshotPath != null) content.append("Screenshot: ").append(screenshotPath).append("\n");
                    if (pageSourcePath != null) content.append("HTML: ").append(pageSourcePath).append("\n");
                    
                    boolean isErrorPrefix = prefix.toLowerCase().contains("error");
                    notificationService.sendNotification(
                        "EasySlot " + (isErrorPrefix ? "Error" : "Debug") + " Information", 
                        content.toString()
                    );
                    logger.info("Email notification sent (interval: {} minutes)", emailInterval / 60000);
                } else {
                    logger.info("Skipping email notification, only {} seconds since last send", (currentTime - lastEmailNotificationTime) / 1000);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to save debug info: {}", e.getMessage());
        }
    }

    /**
     * Automatically start running after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        logger.info("Starting appointment scheduler");

        if (userEmail == null || userPassword == null) {
            logger.warn("Missing user configuration, cannot start scheduler");
            return;
        }

        // Update state
        String dateRange = "N/A";
        String location = "N/A";

        if (appointmentConfig != null) {
            dateRange = String.format("%s ~ %s",
                appointmentConfig.getStartDate(),
                appointmentConfig.getEndDate());
            location = appointmentConfig.getLocation();
        }

        stateManager.updateState(
            userEmail,
            "starting",
            dateRange,
            location,
            false,
            "System is starting"
        );

        try {
            if (isRunning) {
                logger.warn("Scheduler already running");
                return;
            }

            isRunning = true;

            // Send startup notification if debug is disabled
            if (config.getDebug() == null || !config.getDebug().isEnabled()) {
                sendStartupNotification(dateRange, location);
            }

            // Login system
            login();

            // Start monitoring appointments
            startMonitoring();

            logger.info("Appointment scheduler started successfully");

        } catch (Exception e) {
            logger.error("Scheduler startup failed: {}", e.getMessage());
            stateManager.updateState(
                userEmail,
                "error",
                dateRange,
                location,
                false,
                "Error: " + e.getMessage()
            );
            
            // Send error notification without exception details
            sendErrorNotification("Scheduler startup failed", dateRange, location);
        }
    }

    /**
     * Start monitoring appointments
     */
    private void startMonitoring() {
        Thread monitoringThread = new Thread(() -> {
            try {
                logger.info("Starting to monitor appointments");

                while (isRunning) {
                    try {
                        // Navigate to appointment page
                        navigateToReschedule();

                        // Check available dates
                        checkAvailableDates();

                        // Wait for a while before checking again
                        int checkInterval = config.getMonitoring() != null ?
                            config.getMonitoring().getCheckIntervalSeconds() : 300;
                        logger.info("Waiting {} seconds before checking again", checkInterval);
                        Thread.sleep(checkInterval * 1000L);

                    } catch (Exception e) {
                        logger.error("Error occurred during monitoring: {}", e.getMessage());
                        saveDebugInfo("monitor_error");
                        
                        // Send error notification without exception details
                        sendErrorNotification("Error occurred during monitoring", 
                            appointmentConfig != null ? appointmentConfig.getStartDate() + " ~ " + appointmentConfig.getEndDate() : "N/A", 
                            appointmentConfig != null ? appointmentConfig.getLocation() : "N/A");

                        // Wait for a while before retrying
                        int retryInterval = config.getMonitoring() != null ?
                            config.getMonitoring().getErrorRetryIntervalSeconds() : 60;
                        Thread.sleep(retryInterval * 1000L);
                    }
                }
            } catch (Exception e) {
                logger.error("Monitoring thread terminated abnormally: {}", e.getMessage());
                
                // Send error notification without exception details
                sendErrorNotification("Monitoring thread terminated abnormally", 
                    appointmentConfig != null ? appointmentConfig.getStartDate() + " ~ " + appointmentConfig.getEndDate() : "N/A", 
                    appointmentConfig != null ? appointmentConfig.getLocation() : "N/A");
            }
        });

        monitoringThread.setDaemon(true);
        monitoringThread.start();
        logger.info("Monitoring thread started");
    }

    /**
     * Find appointment card matching IVR account
     * @return Matching card element or parent element of continue button
     */
    private WebElement findAppointmentCard() {
        String ivrNumber = appointmentConfig != null ? appointmentConfig.getIvrNumber() : "";
        logger.info("Finding appointment card for IVR account: {}", ivrNumber);

        try {
            // Wait for application card or continue link to appear, increase wait time
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            try {
                // First try to find application card
                wait.until(ExpectedConditions.presenceOfElementLocated(By.className("application")));
                List<WebElement> cards = driver.findElements(By.className("application"));
                logger.info("Found {} appointment cards", cards.size());

                for (WebElement card : cards) {
                    if (card.getText().contains("IVR Account Number: " + ivrNumber)) {
                        logger.info("Found matching IVR account card");
                        return card;
                    }
                }
            } catch (TimeoutException e) {
                // If application card is not found, try multiple ways to find continue link
                logger.info("Appointment card not found, trying multiple ways to find continue link...");
                
                // Method 1: Find by link text
                try {
                    WebElement continueLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Continue")));
                    if (continueLink != null) {
                        logger.info("Found continue link by linkText");
                        return continueLink.findElement(By.xpath("./.."));
                    }
                } catch (Exception e1) {
                    logger.info("Found continue link by linkText, trying other way");
                }

                // Method 2: Find by partial link text
                try {
                    WebElement continueLink = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Continue")));
                    if (continueLink != null) {
                        logger.info("Found continue link by partialLinkText");
                        return continueLink.findElement(By.xpath("./.."));
                    }
                } catch (Exception e2) {
                    logger.info("Found continue link by partialLinkText, trying other way");
                }

                // Method 3: Find by CSS selector
                try {
                    WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a.button.primary.small")));
                    if (continueButton != null && continueButton.getText().contains("Continue")) {
                        logger.info("Found continue button by CSS selector");
                        return continueButton.findElement(By.xpath("./.."));
                    }
                } catch (Exception e3) {
                    logger.info("Found continue button by CSS selector, trying other way");
                }

                // Method 4: Find all links, check text
                try {
                    List<WebElement> allLinks = driver.findElements(By.tagName("a"));
                    for (WebElement link : allLinks) {
                        if (link.getText().trim().equals("Continue")) {
                            logger.info("Found Continue button in all links");
                            return link.findElement(By.xpath("./.."));
                        }
                    }
                } catch (Exception e4) {
                    logger.info("No Continue button found in all links");
                }
                
                // If page exists "Sign in" form, it means possibly need to re-login
                try {
                    if (driver.findElements(By.id("sign_in_form")).size() > 0) {
                        logger.info("Detected login form, need to re-login");
                        throw new RuntimeException("Detected login form, need to re-login");
                    }
                } catch (Exception e5) {
                    // Ignore exception, continue trying other methods
                }
                
                // Save current page for debugging
                saveDebugInfo("continue_search_failed");
                throw new RuntimeException("Unable to find continue link or button");
            }
        } catch (Exception e) {
            logger.error("Error searching for appointment card: {}", e.getMessage());
            saveDebugInfo("search_card_error");
        }

        return null;
    }

    /**
     * Handle reschedule action
     */
    private void handleRescheduleAction() throws InterruptedException {
        logger.info("Finding reschedule option...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("accordion")));

        List<WebElement> accordionItems = driver.findElements(
            By.cssSelector(".accordion-item a.accordion-title")
        );
        for (WebElement item : accordionItems) {
            if (item.getText().contains("Reschedule Appointment")) {
                // Ensure accordion item is visible and clickable
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", item);
                wait.until(ExpectedConditions.elementToBeClickable(item));
                item.click();
                logger.info("Clicked reschedule option");

                // Wait for accordion content to be visible
                WebElement content = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'accordion-content') and .//a[contains(text(), 'Reschedule')]]")
                ));

                // Find and wait for reschedule link to be clickable
                WebElement rescheduleLink = wait.until(ExpectedConditions.elementToBeClickable(
                    content.findElement(By.cssSelector("a.button.small.primary"))
                ));

                // Ensure link is visible in viewport
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", rescheduleLink);
                Thread.sleep(500); // Scroll briefly before pausing
                
                rescheduleLink.click();
                logger.info("Clicked reschedule link");
                return;
            }
        }

        throw new RuntimeException("Unable to find reschedule option");
    }

    /**
     * Navigate to reschedule page
     */
    private void navigateToReschedule() {
        try {
            logger.info("Navigating to appointment page");

            // Save current state for debugging
            saveDebugInfo("before_navigate");

            // Check current URL
            String currentUrl = driver.getCurrentUrl();
            logger.info("Current URL: {}", currentUrl);

            // Check if already on reschedule page
            if (currentUrl.toLowerCase().contains("reschedule") ||
                currentUrl.toLowerCase().contains("appointment") &&
                driver.getPageSource().contains("appointments_consulate_appointment_facility_id")) {
                logger.info("Already on appointment page");
                return;
            }

            // Check if on login page, if so try to login
            if (currentUrl.contains("/users/sign_in") || driver.getPageSource().contains("user_email") && driver.getPageSource().contains("user_password")) {
                logger.info("Detected login page, trying to re-login");
                login();
                // Login successful, re-get current URL
                currentUrl = driver.getCurrentUrl();
                logger.info("Re-login URL: {}", currentUrl);
            }

            // Save page screenshot, for better debugging
            saveDebugInfo("after_login");

            // Check if on group selection page
            if (currentUrl.contains("/groups/") || currentUrl.contains("/schedule")) {
                logger.info("Detected group page, finding matching appointment card");

                // Find and click continue button on matching appointment card
                WebElement targetCard = findAppointmentCard();
                if (targetCard == null) {
                    // Check if need to re-login
                    if (driver.getPageSource().contains("sign_in_form")) {
                        logger.info("Detected need to re-login, trying...");
                        login();
                        // Re-find appointment card
                        targetCard = findAppointmentCard();
                        if (targetCard == null) {
                            saveDebugInfo("no_matching_card_after_relogin");
                            throw new RuntimeException(
                                String.format("Re-login, still unable to find matching IVR account %s appointment card",
                                    appointmentConfig != null ? appointmentConfig.getIvrNumber() : "unknown")
                            );
                        }
                    } else {
                        saveDebugInfo("no_matching_card");
                        throw new RuntimeException(
                            String.format("Unable to find matching IVR account %s appointment card",
                                appointmentConfig != null ? appointmentConfig.getIvrNumber() : "unknown")
                        );
                    }
                }

                // Try to find continue button in different ways
                WebElement continueButton = null;
                try {
                    continueButton = targetCard.findElement(By.cssSelector("a.button.primary.small"));
                } catch (Exception e) {
                    try {
                        continueButton = targetCard.findElement(By.linkText("Continue"));
                    } catch (Exception e2) {
                        try {
                            continueButton = driver.findElement(By.linkText("Continue"));
                        } catch (Exception e3) {
                            try {
                                // Try to find any button
                                List<WebElement> buttons = targetCard.findElements(By.tagName("a"));
                                if (!buttons.isEmpty()) {
                                    continueButton = buttons.get(0);
                                }
                            } catch (Exception e4) {
                                // Try to find all continue links directly on page
                                List<WebElement> allContinueLinks = driver.findElements(By.linkText("Continue"));
                                if (!allContinueLinks.isEmpty()) {
                                    continueButton = allContinueLinks.get(0);
                                    logger.info("Found Continue link on page");
                                }
                            }
                        }
                    }
                }

                if (continueButton == null) {
                    throw new RuntimeException("Unable to find continue button");
                }

                // Ensure button is visible and clickable
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", continueButton);
                Thread.sleep(500);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                wait.until(ExpectedConditions.elementToBeClickable(continueButton));

                try {
                    continueButton.click();
                    logger.info("Clicked continue button");
                } catch (Exception e) {
                    // If normal click fails, try JavaScript click
                    logger.info("Normal click failed, trying JavaScript click");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueButton);
                    logger.info("Used JavaScript to click continue button");
                }
                Thread.sleep(2000);
            }

            // Save state for debugging
            saveDebugInfo("after_continue_check");

            // Handle reschedule action
            try {
                handleRescheduleAction();

                // Verify if reached reschedule page
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("appointments_consulate_appointment_facility_id")
                ));
                logger.info("Successfully navigated to reschedule page");
                return;
            } catch (Exception e) {
                logger.error("Failed to handle reschedule action: {}", e.getMessage());
                saveDebugInfo("reschedule_action_error");

                // Check if need to re-login
                if (driver.getPageSource().contains("sign_in_form")) {
                    logger.info("Detected need to re-login, trying...");
                    login();
                    // Re-try navigate
                    navigateToReschedule();
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Error navigating to appointment page: {}", e.getMessage());
            saveDebugInfo("navigate_error");
            throw new RuntimeException("Error navigating to appointment page", e);
        }
    }

    /**
     * Check available dates
     */
    private void checkAvailableDates() {
        try {
            logger.info("Checking available dates");

            // Update state to indicate we're checking for appointments
            String dateRange = appointmentConfig != null ? 
                String.format("%s ~ %s", appointmentConfig.getStartDate(), appointmentConfig.getEndDate()) : "N/A";
            String currentLocation = appointmentConfig != null ? appointmentConfig.getLocation() : "N/A";
            stateManager.updateState(
                userEmail,
                "checking",
                dateRange,
                currentLocation,
                false,
                "Checking for available appointments"
            );

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            // Select appointment location
            if (appointmentConfig != null && appointmentConfig.getLocation() != null) {
                WebElement locationSelect = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("appointments_consulate_appointment_facility_id")));

                locationSelect.click();
                Thread.sleep(1000);
            
                // Select specified location
                String location = appointmentConfig.getLocation();
                List<WebElement> options = locationSelect.findElements(By.tagName("option"));
                for (WebElement option : options) {
                    if (option.getText().trim().contains(location)) {
                        option.click();
                        logger.info("Selected location: {}", location);
                        Thread.sleep(2000);
                        break;
                    }
                }

                // Check if page displays "System is busy" error
                if (isSystemBusyError()) {
                    logger.warn("Detected 'System is busy' error, waiting for next check loop");
                    return; // Directly return, waiting for next check
                }
            }

            // Click date input box, show calendar
            try {
                WebElement dateInput = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("appointments_consulate_appointment_date")));
                dateInput.click();

                // Wait for calendar to appear
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("ui-datepicker-calendar")));
            } catch (Exception e) {
                // Check again if it's "System is busy" error
                if (isSystemBusyError()) {
                    logger.warn("Clicked date input box, detected 'System is busy' error, waiting for next check loop");
                    return; // Directly return, waiting for next check
                }
                throw e; // If not system busy error, continue throwing exception
            }

            // Find available dates (non-grayed out dates)
            List<WebElement> availableDates = driver.findElements(
                By.cssSelector(".ui-datepicker-calendar td:not(.ui-datepicker-unselectable) a"));

            if (availableDates.isEmpty()) {
                logger.info("No available dates found");
                return;
            }

            logger.info("Found {} available dates", availableDates.size());

            // Check for available dates
            List<AppointmentResult> results = new ArrayList<>();

            for (WebElement dateElement : availableDates) {
                String dateText = dateElement.getAttribute("data-date");
                if (dateText == null) {
                    dateText = dateElement.getText();
                }

                logger.info("Found available date: {}", dateText);

                try {
                    // Try clicking date to view available time
                    dateElement.click();
                    Thread.sleep(1000);

                    // Check if system busy error appears
                    if (isSystemBusyError()) {
                        logger.warn("Clicked date, detected 'System is busy' error, trying next date");
                        continue; // Skip current date, try next
                    }

                    // Get available time
                    WebElement timeSelect = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.id("appointments_consulate_appointment_time")));

                    List<WebElement> timeOptions = timeSelect.findElements(By.tagName("option"));
                    List<String> availableTimes = new ArrayList<>();

                    // Skip first option (usually "Select...")
                    for (int i = 1; i < timeOptions.size(); i++) {
                        String timeText = timeOptions.get(i).getText();
                        availableTimes.add(timeText);
                        logger.info("Found available time: {}", timeText);
                    
                        // Create appointment result object
                        AppointmentResult result = new AppointmentResult();
                        result.setCity(appointmentConfig.getLocation());
                        result.setDate(dateText);
                        result.setTime(timeText);
                        result.setAutoBooked(false); // Do not auto-book for now

                        results.add(result);
                    }
                } catch (Exception e) {
                    // If system busy error, log and continue
                    if (isSystemBusyError()) {
                        logger.warn("Detected 'System is busy' error while handling date, trying next date");
                        continue;
                    }
                    logger.error("Error processing date {}: {}", dateText, e.getMessage());
                }
            }

            // If available appointments found, send notification
            if (!results.isEmpty()) {
                logger.info("Found a total of {} available appointment time slots", results.size());

                // Send notification for each available appointment
                for (AppointmentResult result : results) {
                    notifyAvailableAppointment(result);
                }

                // Update status
                stateManager.updateState(
                    userEmail,
                    "available",
                    results.get(0).getDate(),
                    results.get(0).getCity(),
                    true,
                    "Found available appointments: " + results.size()
                );
            } else {
                // Update status
                stateManager.updateState(
                    userEmail,
                    "unavailable",
                    "N/A",
                    appointmentConfig.getLocation(),
                    false,
                    "No available appointments found"
                );
            }

        } catch (Exception e) {
            // Final check if system busy error
            if (isSystemBusyError()) {
                logger.warn("Detected 'System is busy' error during checking available dates, waiting for next check cycle");
                saveDebugInfo("system_busy");
            } else {
                logger.error("Error checking available dates: {}", e.getMessage());
                saveDebugInfo("check_dates_error");
                
                // Send error notification without exception details
                sendErrorNotification("Error checking available dates", 
                    appointmentConfig != null ? appointmentConfig.getStartDate() + " ~ " + appointmentConfig.getEndDate() : "N/A",
                    appointmentConfig != null ? appointmentConfig.getLocation() : "N/A");
            }
        }
    }
    
    /**
     * Check if page displays "System is busy" error
     * @return true if system busy error detected
     */
    private boolean isSystemBusyError() {
        try {
            String pageSource = driver.getPageSource().toLowerCase();
            boolean systemBusy = pageSource.contains("system is busy") || 
                                pageSource.contains("please try again later") ||
                                pageSource.contains("system busy") ||
                                pageSource.contains("try again later");
                                
            // Check if error messages on page
            List<WebElement> errorMessages = driver.findElements(By.className("error-message"));
            for (WebElement error : errorMessages) {
                String errorText = error.getText().toLowerCase();
                if (errorText.contains("system is busy") || errorText.contains("please try again later")) {
                    return true;
                }
            }
            
            // Check alert messages
            List<WebElement> alerts = driver.findElements(By.className("alert"));
            for (WebElement alert : alerts) {
                String alertText = alert.getText().toLowerCase();
                if (alertText.contains("system is busy") || alertText.contains("please try again later")) {
                    return true;
                }
            }
            
            return systemBusy;
        } catch (Exception e) {
            logger.debug("Error checking system busy status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send notification for available appointment
     * 
     * @param result Appointment result containing details
     */
    private void notifyAvailableAppointment(AppointmentResult result) {
        String subject = "Available Appointment Found!";
        StringBuilder content = new StringBuilder();
        content.append("EasySlot found an available appointment:\n\n");
        content.append("Location: ").append(result.getCity()).append("\n");
        content.append("Date: ").append(result.getDate()).append("\n");
        content.append("Time: ").append(result.getTime()).append("\n\n");
        
        if (result.isAutoBooked()) {
            content.append("Status: AUTOMATICALLY BOOKED!\n");
        } else {
            content.append("Status: Available (not booked)\n");
        }
        
        // Available appointment notifications are always sent, not subject to frequency limitation
        notificationService.sendNotification(subject, content.toString());
        logger.info("Sent notification about available appointment");
    }
    
    /**
     * Send startup notification with key search information
     * 
     * @param dateRange Date range for the search
     * @param location Location for the search
     */
    private void sendStartupNotification(String dateRange, String location) {
        String subject = "EasySlot Search Started";
        StringBuilder content = new StringBuilder();
        content.append("EasySlot has started searching for appointments with the following criteria:\n\n");
        content.append("User: ").append(userEmail).append("\n");
        content.append("Location: ").append(location).append("\n");
        content.append("Date Range: ").append(dateRange).append("\n");
        
        if (appointmentConfig != null && appointmentConfig.getIvrNumber() != null) {
            content.append("IVR Number: ").append(appointmentConfig.getIvrNumber()).append("\n");
        }
        
        content.append("\nYou will be notified when available appointments are found.");
        
        notificationService.sendNotification(subject, content.toString());
        logger.info("Sent startup notification");
    }
    
    /**
     * Send error notification without exception details
     * 
     * @param errorMessage Brief error message
     * @param dateRange Date range for the search
     * @param location Location for the search
     */
    private void sendErrorNotification(String errorMessage, String dateRange, String location) {
        // Only send notification if debug is disabled
        if (config.getDebug() != null && config.getDebug().isEnabled()) {
            return;
        }
        
        String subject = "EasySlot Error";
        StringBuilder content = new StringBuilder();
        content.append("EasySlot encountered an error while searching for appointments:\n\n");
        content.append("Error: ").append(errorMessage).append("\n\n");
        content.append("Search Details:\n");
        content.append("User: ").append(userEmail).append("\n");
        content.append("Location: ").append(location).append("\n");
        content.append("Date Range: ").append(dateRange).append("\n\n");
        content.append("The system will continue trying. You will be notified of any further updates.");
        
        notificationService.sendNotification(subject, content.toString());
        logger.info("Sent error notification");
    }
} 