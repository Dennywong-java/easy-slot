"""Browser driver management module."""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.chrome.service import Service as ChromeService
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.edge.service import Service as EdgeService
from selenium.webdriver.firefox.options import Options as FirefoxOptions
from selenium.webdriver.firefox.service import Service as FirefoxService
from webdriver_manager.chrome import ChromeDriverManager
from webdriver_manager.firefox import GeckoDriverManager
from webdriver_manager.microsoft import EdgeChromiumDriverManager


class BrowserFactory:
    """Factory class for creating and configuring browser drivers."""

    @staticmethod
    def create_driver(config):
        """Create and configure a browser driver based on configuration.

        Args:
            config (dict): Browser configuration dictionary.

        Returns:
            WebDriver: Configured browser driver instance.
        """
        browser_type = config.get("type", "chrome").lower()
        headless = config.get("headless", False)

        if browser_type == "chrome":
            return BrowserFactory._setup_chrome_driver(headless)
        elif browser_type == "firefox":
            return BrowserFactory._setup_firefox_driver(headless)
        elif browser_type == "edge":
            return BrowserFactory._setup_edge_driver(headless)
        else:
            raise ValueError(f"Unsupported browser type: {browser_type}")

    @staticmethod
    def _setup_chrome_driver(headless=False):
        """Set up Chrome WebDriver with specified options.

        Args:
            headless (bool): Whether to run browser in headless mode.

        Returns:
            WebDriver: Configured Chrome WebDriver instance.
        """
        options = ChromeOptions()
        if headless:
            options.add_argument("--headless")

        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument("--window-size=1920,1080")
        options.add_argument("--start-maximized")

        service = ChromeService(executable_path=ChromeDriverManager().install())

        return webdriver.Chrome(service=service, options=options)

    @staticmethod
    def _setup_firefox_driver(headless=False):
        """Set up Firefox WebDriver with specified options.

        Args:
            headless (bool): Whether to run browser in headless mode.

        Returns:
            WebDriver: Configured Firefox WebDriver instance.
        """
        options = FirefoxOptions()
        if headless:
            options.add_argument("--headless")

        options.add_argument("--width=1920")
        options.add_argument("--height=1080")

        service = FirefoxService(executable_path=GeckoDriverManager().install())

        return webdriver.Firefox(service=service, options=options)

    @staticmethod
    def _setup_edge_driver(headless=False):
        """Set up Edge WebDriver with specified options.

        Args:
            headless (bool): Whether to run browser in headless mode.

        Returns:
            WebDriver: Configured Edge WebDriver instance.
        """
        options = EdgeOptions()
        if headless:
            options.add_argument("--headless")

        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument("--window-size=1920,1080")
        options.add_argument("--start-maximized")

        service = EdgeService(executable_path=EdgeChromiumDriverManager().install())

        return webdriver.Edge(service=service, options=options)
