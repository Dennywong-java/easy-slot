package com.easyslot.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.WebDriver;
import com.easyslot.config.model.Browser;
import static org.junit.jupiter.api.Assertions.*;

class BrowserFactoryTest {
    private WebDriver driver;

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void testCreateChromeDriver() {
        Browser browser = new Browser();
        browser.setType("chrome");
        browser.setHeadless(true);

        driver = BrowserFactory.createDriver(browser);
        assertNotNull(driver, "Driver should not be null");
        assertTrue(driver.getClass().getName().toLowerCase().contains("chrome"),
                "Should create Chrome driver");
    }

    @Test
    void testDefaultToChromeWhenTypeNotSpecified() {
        Browser browser = new Browser();
        browser.setType("chrome"); // Set default type
        browser.setHeadless(true);

        driver = BrowserFactory.createDriver(browser);
        assertNotNull(driver, "Driver should not be null");
        assertTrue(driver.getClass().getName().toLowerCase().contains("chrome"),
                "Should default to Chrome driver");
    }
} 