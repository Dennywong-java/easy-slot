"""Main entry point for the appointment scheduler."""

from .scheduler import AppointmentScheduler
from .utils.logging_config import setup_logging


def main():
    """Execute the main program logic."""
    setup_logging()
    scheduler = AppointmentScheduler()
    scheduler.run()


if __name__ == "__main__":
    main()
