"""Easy-slot package for visa appointment scheduling automation."""

from .scheduler import VisaScheduler
from .utils.logging_config import setup_logging

__all__ = ["VisaScheduler", "setup_logging"]

__version__ = "1.0.0"
