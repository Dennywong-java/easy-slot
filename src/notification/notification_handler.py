"""Email notification handling module."""

from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import logging
import smtplib


class NotificationHandler:
    """Handler for sending email notifications."""

    def __init__(self, config):
        """Initialize notification handler with configuration.

        Args:
            config (dict): Configuration dictionary containing email settings.
        """
        self.config = config
        self.smtp_config = config.get("smtp", {})
        self.recipients = config.get("notification", {}).get("recipients", [])

    def notify(self, message):
        """Send a notification message.

        Args:
            message (str): Message content to send.
        """
        if not self.recipients:
            logging.warning("No recipients configured for notifications")
            return

        try:
            msg = MIMEMultipart()
            msg["From"] = self.smtp_config.get("username")
            msg["To"] = ", ".join(self.recipients)
            msg["Subject"] = "Appointment Notification"
            msg.attach(MIMEText(message, "plain"))

            with smtplib.SMTP(self.smtp_config.get("server"), self.smtp_config.get("port")) as server:
                server.starttls()
                server.login(self.smtp_config.get("username"), self.smtp_config.get("password"))
                server.send_message(msg)
                logging.info("Notification sent successfully")
        except Exception as e:
            logging.error(f"Failed to send notification: {str(e)}")

    def notify_appointment_found(self, city, date):
        """Send notification for found appointment.

        Args:
            city (str): City where appointment is available.
            date (str): Available appointment date.
        """
        message = f"Found available appointment!\n" f"Location: {city}\n" f"Date: {date}"
        self.notify(message)

    def notify_start_monitoring(self, cities, date_range, mode):
        """Send notification when monitoring starts.

        Args:
            cities (list): List of cities being monitored.
            date_range (dict): Date range being monitored.
            mode (str): Monitoring mode description.
        """
        message = (
            f"Started monitoring appointments\n"
            f"Cities: {', '.join(cities)}\n"
            f"Date Range: {date_range['start_date']} to {date_range['end_date']}\n"
            f"Mode: {mode}"
        )
        self.notify(message)

    def notify_error(self, error_message):
        """Send notification for errors.

        Args:
            error_message (str): Error message to send.
        """
        message = f"Error occurred: {error_message}"
        self.notify(message)
