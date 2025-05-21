"""Main entry point for the visa appointment scheduler."""

from .scheduler import VisaScheduler
from .utils.logging_config import setup_logging


def main():
    """Execute the main program logic."""
    setup_logging()
    scheduler = VisaScheduler()
    scheduler.run()


if __name__ == "__main__":
    main()
