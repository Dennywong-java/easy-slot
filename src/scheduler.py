"""Appointment scheduling automation module."""

from datetime import datetime
import logging
from pathlib import Path
import time

from retry import retry
from selenium.common.exceptions import NoSuchElementException, TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from .browser.drivers import BrowserFactory
from .config.config_loader import ConfigLoader
from .notification.notification_handler import NotificationHandler
from .utils.date_utils import DateUtils


class AppointmentScheduler:
    """Main class for handling consular appointment scheduling."""

    def __init__(self, config_path="config.yml"):
        """Initialize the scheduler with configuration.

        Args:
            config_path (str): Path to the configuration file.
        """
        self.config = ConfigLoader.load_config(config_path)
        ConfigLoader.validate_config(self.config)

        self.driver = None
        self.base_url = "https://ais.usvisa-info.com/en-ca/niv"
        self.notification = NotificationHandler(self.config)

        # Setup logs directory
        self.logs_dir = Path("logs")
        self._setup_logging()
        self._cleanup_old_files()

    def _setup_logging(self):
        """Set up logging directory and configuration."""
        # Create logs directory if it doesn't exist
        self.logs_dir.mkdir(exist_ok=True)

        # Configure logging to write to file in logs directory
        log_file = self.logs_dir / "scheduler.log"

        # Configure logging to append rather than overwrite
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s - %(levelname)s - %(message)s",
            handlers=[
                logging.FileHandler(log_file, mode="a"),  # Changed to append mode
                logging.StreamHandler(),  # Also log to console
            ],
        )
        logging.info("Logging initialized")

    def _cleanup_old_files(self):
        """Clean up old screenshot and log files."""
        try:
            # Remove old files in logs directory
            patterns = ["login_error_*.png", "page_state_*.png", "page_source_*.html"]

            for pattern in patterns:
                files = self.logs_dir.glob(pattern)
                for file in files:
                    try:
                        file.unlink()
                        logging.info(f"Removed old file: {file}")
                    except OSError as e:
                        logging.warning(f"Failed to remove file {file}: {str(e)}")

            # Don't clear log file anymore, just keep appending
            logging.info("Starting new session")

        except Exception as e:
            logging.warning(f"Error during cleanup: {str(e)}")

    def _handle_important_info_page(self):
        """Handle the Important Information overlay page."""
        try:
            logging.info("Checking for Important Information page...")
            down_arrow = WebDriverWait(self.driver, 5).until(
                EC.presence_of_element_located((By.CLASS_NAME, "down-arrow"))
            )
            down_arrow.click()
            logging.info("Clicked down arrow on Important Information page")
            return True
        except TimeoutException:
            logging.info("No Important Information page found")
            return False

    def _handle_login_form(self):
        """Handle the login form submission."""
        logging.info("Looking for login form...")
        WebDriverWait(self.driver, 5).until(
            EC.presence_of_element_located((By.ID, "sign_in_form"))
        )
        logging.info("Login form found")

        # Fill in email
        logging.info("Filling email field...")
        email_field = WebDriverWait(self.driver, 5).until(
            EC.presence_of_element_located((By.ID, "user_email"))
        )
        email_field.clear()
        email_field.send_keys(self.config["credentials"]["email"])
        logging.info("Email entered successfully")

        # Fill in password
        logging.info("Filling password field...")
        password_field = WebDriverWait(self.driver, 5).until(
            EC.presence_of_element_located((By.ID, "user_password"))
        )
        password_field.clear()
        password_field.send_keys(self.config["credentials"]["password"])
        logging.info("Password entered successfully")

        # Handle privacy policy checkbox
        self._handle_privacy_checkbox()

        # Click Sign In button
        sign_in_button = WebDriverWait(self.driver, 5).until(
            EC.element_to_be_clickable(
                (By.CSS_SELECTOR, "input.button.primary[name='commit']")
            )
        )
        sign_in_button.click()
        logging.info("Clicked sign in button")

    def _handle_privacy_checkbox(self):
        """Handle the privacy policy checkbox selection."""
        try:
            logging.info("Handling privacy policy checkbox...")
            checkbox = WebDriverWait(self.driver, 5).until(
                EC.presence_of_element_located((By.NAME, "policy_confirmed"))
            )
            if not checkbox.is_selected():
                checkbox_container = self.driver.find_element(
                    By.CLASS_NAME, "icheckbox"
                )
                checkbox_container.click()
                if not checkbox.is_selected():
                    checkbox.click()
                    if not checkbox.is_selected():
                        self.driver.execute_script(
                            "document.querySelector('input[name=\"policy_confirmed\"]').checked = true;"
                            "document.querySelector('input[name=\"policy_confirmed\"]').dispatchEvent("
                            "new Event('change', { bubbles: true }));"
                        )
            logging.info("Privacy policy checkbox handled")
        except Exception as e:
            logging.error(f"Failed to handle privacy checkbox: {str(e)}")
            raise

    @retry(TimeoutException, tries=3, delay=2)
    def login(self):
        """Log in to the appointment system."""
        logging.info("Attempting to log in...")
        try:
            # Load the login page
            logging.info(f"Loading login page: {self.base_url}/users/sign_in")
            self.driver.get(f"{self.base_url}/users/sign_in")

            # Handle Important Information page and login form
            self._handle_important_info_page()
            self._handle_login_form()

            # Verify login success
            success_indicators = [
                (By.LINK_TEXT, "Continue"),
                (By.CLASS_NAME, "application"),
                (By.CLASS_NAME, "consular-appt"),
            ]

            login_success = False
            for locator in success_indicators:
                try:
                    WebDriverWait(self.driver, 3).until(
                        EC.presence_of_element_located(locator)
                    )
                    login_success = True
                    logging.info(
                        f"Successfully logged in - found indicator: {locator[1]}"
                    )
                    break
                except TimeoutException:
                    continue

            if not login_success:
                error_messages = self.driver.find_elements(
                    By.CLASS_NAME, "error-message"
                )
                if error_messages:
                    error_text = error_messages[0].text
                    logging.error(f"Login failed - error message found: {error_text}")
                    raise Exception(f"Login failed: {error_text}")

                logging.error("Login failed - could not detect any success indicators")
                self._save_debug_info("login_failed")
                raise Exception("Login failed - could not verify success")

        except Exception as e:
            error_msg = f"Login failed: {str(e)}"
            logging.error(error_msg)
            self._save_debug_info("login_error")
            raise

    def _save_debug_info(self, prefix):
        """Save debug information including screenshot and page source."""
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            screenshot_path = self.logs_dir / f"{prefix}_{timestamp}.png"
            self.driver.save_screenshot(str(screenshot_path))
            logging.info(f"Screenshot saved to {screenshot_path}")

            page_source_path = self.logs_dir / f"page_source_{prefix}_{timestamp}.html"
            with open(page_source_path, "w", encoding="utf-8") as f:
                f.write(self.driver.page_source)
            logging.info(f"Page source saved to {page_source_path}")
        except Exception as e:
            logging.error(f"Failed to save debug information: {str(e)}")

    def _find_appointment_card(self):
        """Find and return the appointment card with matching IVR number."""
        ivr_number = self.config["appointment"]["ivr_number"]
        logging.info(f"Looking for appointment with IVR: {ivr_number}")

        WebDriverWait(self.driver, 10).until(
            EC.presence_of_element_located((By.CLASS_NAME, "application"))
        )

        cards = self.driver.find_elements(By.CLASS_NAME, "application")
        logging.info(f"Found {len(cards)} appointment cards")

        for card in cards:
            if f"IVR Account Number: {ivr_number}" in card.text:
                logging.info("Found card with matching IVR number")
                return card

        return None

    def navigate_to_reschedule(self):
        """Navigate to the reschedule appointment page."""
        try:
            # Check if already on reschedule page
            current_url = self.driver.current_url
            if "reschedule" in current_url.lower():
                logging.info("Already on reschedule page")
                return True

            # Find and click Continue button on matching appointment card
            target_card = self._find_appointment_card()
            if not target_card:
                self._save_debug_info("no_matching_card")
                raise Exception(
                    f"Could not find appointment card with IVR: {self.config['appointment']['ivr_number']}"
                )

            continue_button = target_card.find_element(
                By.CSS_SELECTOR, "a.button.primary.small"
            )
            if not continue_button.is_displayed():
                self.driver.execute_script(
                    "arguments[0].scrollIntoView(true);", continue_button
                )
            continue_button.click()
            logging.info("Clicked Continue button")

            # Handle action selection page
            self._handle_reschedule_action()

            # Verify arrival at reschedule page
            WebDriverWait(self.driver, 5).until(
                EC.presence_of_element_located(
                    (By.ID, "appointments_consulate_appointment_facility_id")
                )
            )
            logging.info("Successfully navigated to reschedule page")
            return True

        except Exception as e:
            error_msg = f"Failed to navigate to reschedule page: {str(e)}"
            logging.error(error_msg)
            self._save_debug_info("navigation_error")
            self.notification.notify_error(error_msg)
            return False

    def _handle_reschedule_action(self):
        """Handle the reschedule action selection."""
        logging.info("Looking for Reschedule Appointment option...")
        WebDriverWait(self.driver, 5).until(
            EC.presence_of_element_located((By.CLASS_NAME, "accordion"))
        )

        accordion_items = self.driver.find_elements(
            By.CSS_SELECTOR, ".accordion-item a.accordion-title"
        )
        for item in accordion_items:
            if "Reschedule Appointment" in item.text:
                item.click()
                logging.info("Clicked Reschedule Appointment accordion")

                content = item.find_element(
                    By.XPATH,
                    "following-sibling::div[contains(@class, 'accordion-content')]",
                )
                reschedule_link = content.find_element(
                    By.CSS_SELECTOR, "a.button.small.primary"
                )
                reschedule_link.click()
                logging.info("Clicked Reschedule Appointment link")
                return

        raise Exception("Could not find Reschedule Appointment option")

    def check_appointments(self):
        """Check for available appointment slots."""
        try:
            WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located(
                    (By.ID, "appointments_consulate_appointment_facility_id")
                )
            )

            facilities = self.driver.find_element(
                By.ID, "appointments_consulate_appointment_facility_id"
            )
            facility_options = facilities.find_elements(By.TAG_NAME, "option")

            for facility in facility_options:
                city_name = facility.text.strip()
                if city_name in self.config["appointment"]["preferred_cities"]:
                    facility.click()
                    time.sleep(2)  # Wait for calendar to update

                    try:
                        date_picker = self.driver.find_element(
                            By.ID, "appointments_consulate_appointment_date"
                        )
                        if "disabled" not in date_picker.get_attribute("class"):
                            available_date = date_picker.get_attribute("value")
                            if DateUtils.is_date_in_range(
                                available_date,
                                self.config["appointment"]["date_range"]["start_date"],
                                self.config["appointment"]["date_range"]["end_date"],
                            ):
                                logging.info(
                                    f"Found available appointment in {city_name} on {available_date}"
                                )
                                self.notification.notify_appointment_found(
                                    city_name, available_date
                                )
                                return True, city_name, available_date
                    except NoSuchElementException:
                        continue

            logging.info("No suitable appointments found")
            return False, None, None

        except Exception as e:
            error_msg = f"Failed to check appointments: {str(e)}"
            self.notification.notify_error(error_msg)
            raise

    def book_appointment(self, city, date):
        """Book the appointment for the given city and date."""
        try:
            logging.info(f"Attempting to book appointment in {city} for {date}")
            time_select = self.driver.find_element(
                By.ID, "appointments_consulate_appointment_time"
            )
            time_options = time_select.find_elements(By.TAG_NAME, "option")

            if time_options:
                time_options[1].click()  # Index 0 is usually the placeholder

                submit_button = self.driver.find_element(By.ID, "appointments_submit")
                submit_button.click()

                WebDriverWait(self.driver, 10).until(
                    EC.presence_of_element_located((By.CLASS_NAME, "confirmation-page"))
                )

                success_message = (
                    f"Successfully booked appointment!\n"
                    f"Location: {city}\n"
                    f"Date: {date}"
                )
                self.notification.notify(success_message)
                return True

            return False

        except Exception as e:
            error_msg = f"Failed to book appointment: {str(e)}"
            self.notification.notify_error(error_msg)
            return False

    def run(self):
        """Execute the main scheduling loop."""
        try:
            self.driver = BrowserFactory.create_driver(self.config["browser"])
            self.driver.implicitly_wait(10)

            self.login()

            if not self.navigate_to_reschedule():
                raise Exception("Failed to navigate to reschedule page")

            auto_book = self.config["appointment"]["auto_book"]
            mode_str = "automatic booking" if auto_book else "notification only"
            self.notification.notify_start_monitoring(
                self.config["appointment"]["preferred_cities"],
                self.config["appointment"]["date_range"],
                mode_str,
            )

            while True:
                success, city, date = self.check_appointments()
                if success:
                    if auto_book:
                        if self.book_appointment(city, date):
                            break
                    else:
                        logging.info("Found appointment but auto-booking is disabled")
                        self.notification.notify_appointment_found(city, date)
                        time.sleep(self.config["monitoring"]["check_interval"])

                logging.info(
                    f"Waiting {self.config['monitoring']['check_interval']} seconds before next check..."
                )
                time.sleep(self.config["monitoring"]["check_interval"])

                self.driver.refresh()
                time.sleep(2)

        except Exception as e:
            error_msg = f"An error occurred: {str(e)}"
            logging.error(error_msg)
        finally:
            if self.driver:
                self.driver.quit()
