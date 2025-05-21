"""Date utility functions for appointment scheduling."""

from datetime import datetime


class DateUtils:
    """Utility class for date operations."""

    @staticmethod
    def is_date_in_range(date_str, start_date_str, end_date_str):
        """Check if a date is within a specified range.

        Args:
            date_str (str): Date to check in YYYY-MM-DD format.
            start_date_str (str): Start date in YYYY-MM-DD format.
            end_date_str (str): End date in YYYY-MM-DD format.

        Returns:
            bool: True if date is within range, False otherwise.
        """
        if not date_str:
            return False

        date = datetime.strptime(date_str, "%Y-%m-%d")
        start_date = datetime.strptime(start_date_str, "%Y-%m-%d")
        end_date = datetime.strptime(end_date_str, "%Y-%m-%d")

        return start_date <= date <= end_date
