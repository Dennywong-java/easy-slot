package com.easyslot.browser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.easyslot.config.model.Browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for creating, configuring, and managing browser drivers.
 */
public class BrowserFactory {
    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);
    private static final Map<String, WebDriver> activeDrivers = new ConcurrentHashMap<>();

    /**
     * Create and configure a browser driver based on configuration.
     *
     * @param browser Browser configuration object
     * @return Configured WebDriver instance
     */
    public static WebDriver createDriver(Browser browser) {
        String browserType = browser != null && browser.getType() != null ? browser.getType() : "chrome";
        boolean headless = browser != null && browser.isHeadless();
        String key = browser.getEmail() != null ? browser.getEmail() : "default";
        
        // Check if driver already exists
        if (activeDrivers.containsKey(key)) {
            try {
                WebDriver existingDriver = activeDrivers.get(key);
                existingDriver.getCurrentUrl(); // Check if browser is still responsive
                return existingDriver;
            } catch (Exception e) {
                logger.info("Existing driver not responsive, creating new one");
                closeDriver(key);
            }
        }

        WebDriver driver;
        switch (browserType.toLowerCase()) {
            case "firefox":
                driver = setupFirefoxDriver(browser);
                break;
            case "edge":
                driver = setupEdgeDriver(browser);
                break;
            default:
                driver = setupChromeDriver(browser);
                break;
        }
        
        activeDrivers.put(key, driver);
        return driver;
    }
    
    /**
     * Close a specific driver by key
     *
     * @param key The key (usually email) of the driver to close
     */
    public static void closeDriver(String key) {
        WebDriver driver = activeDrivers.remove(key);
        if (driver != null) {
            try {
                driver.quit();
                logger.info("Browser closed for: {}", key);
            } catch (Exception e) {
                logger.error("Error closing browser: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Close all active drivers
     */
    public static void closeAllDrivers() {
        activeDrivers.forEach((key, driver) -> {
            try {
                driver.quit();
                logger.info("Browser closed for: {}", key);
            } catch (Exception e) {
                logger.error("Error closing browser for {}: {}", key, e.getMessage());
            }
        });
        activeDrivers.clear();
    }

    /**
     * Set up Chrome WebDriver with specified options.
     *
     * @param browser Browser configuration
     * @return Configured Chrome WebDriver instance
     */
    private static WebDriver setupChromeDriver(Browser browser) {
        ChromeOptions options = new ChromeOptions();
        
        // Basic options
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-notifications");
        
        // Memory and performance optimization
        options.addArguments("--start-maximized");
        options.addArguments("--window-size=1920,1080");
        
        // Headless mode settings
        if (browser.isHeadless()) {
            options.addArguments("--headless=new");
        }
        
        // Set user agent
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.7103.113 Safari/537.36");
        
        // Set binary location from config if available
        Map<String, Object> chromeOptions = browser.getOptions() != null ? 
                                            browser.getOptions().getChrome() : null;
        if (chromeOptions != null && chromeOptions.get("binaryLocation") != null) {
            String binaryLocation = (String) chromeOptions.get("binaryLocation");
            if (!binaryLocation.isEmpty()) {
                options.setBinary(binaryLocation);
            }
        }

        // Create and configure driver
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(60));
        
        return driver;
    }

    /**
     * Set up Firefox WebDriver with specified options.
     *
     * @param browser Browser configuration
     * @return Configured Firefox WebDriver instance
     */
    private static WebDriver setupFirefoxDriver(Browser browser) {
        FirefoxOptions options = new FirefoxOptions();
        
        if (browser.isHeadless()) {
            options.addArguments("--headless");
        }

        // Additional options for stability
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        // Set binary location from config if available
        Map<String, Object> firefoxOptions = browser.getOptions() != null ? 
                                             browser.getOptions().getFirefox() : null;
        if (firefoxOptions != null && firefoxOptions.get("binaryLocation") != null) {
            String binaryLocation = (String) firefoxOptions.get("binaryLocation");
            if (!binaryLocation.isEmpty()) {
                options.setBinary(binaryLocation);
            }
        }

        return new FirefoxDriver(options);
    }

    /**
     * Set up Edge WebDriver with specified options.
     *
     * @param browser Browser configuration
     * @return Configured Edge WebDriver instance
     */
    private static WebDriver setupEdgeDriver(Browser browser) {
        EdgeOptions options = new EdgeOptions();
        
        if (browser.isHeadless()) {
            options.addArguments("--headless");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");

        // Set binary location from config if available
        Map<String, Object> edgeOptions = browser.getOptions() != null ? 
                                         browser.getOptions().getEdge() : null;
        if (edgeOptions != null && edgeOptions.get("binaryLocation") != null) {
            String binaryLocation = (String) edgeOptions.get("binaryLocation");
            if (!binaryLocation.isEmpty()) {
                options.setBinary(binaryLocation);
            }
        }

        return new EdgeDriver(options);
    }
} 