"""Appointment scheduling automation package for consular services."""

from .scheduler import AppointmentScheduler
from .utils.logging_config import setup_logging

__all__ = ["AppointmentScheduler", "setup_logging"]

__version__ = "1.0.0"
