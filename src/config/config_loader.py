"""Configuration loading and validation module."""

import os

import yaml


class ConfigLoader:
    """Configuration loading and validation utility class."""

    @staticmethod
    def load_config(config_path):
        """Load configuration from YAML file.

        Args:
            config_path (str): Path to the configuration file.

        Returns:
            dict: Loaded configuration dictionary.

        Raises:
            FileNotFoundError: If configuration file is not found.
            yaml.YAMLError: If configuration file is invalid.
        """
        if not os.path.exists(config_path):
            raise FileNotFoundError(f"Configuration file not found: {config_path}")

        with open(config_path, "r") as f:
            return yaml.safe_load(f)

    @staticmethod
    def validate_config(config):
        """Validate configuration structure and required fields.

        Args:
            config (dict): Configuration dictionary to validate.

        Raises:
            ValueError: If required configuration fields are missing or invalid.
        """
        required_sections = ["credentials", "browser", "appointment", "monitoring"]
        for section in required_sections:
            if section not in config:
                raise ValueError(f"Missing required configuration section: {section}")

        if not config["credentials"].get("email") or not config["credentials"].get(
            "password"
        ):
            raise ValueError("Missing email or password in credentials section")

        if not config["appointment"].get("preferred_cities"):
            raise ValueError("No preferred cities specified in appointment section")

        required_fields = [
            "credentials.email",
            "credentials.password",
            "appointment.ivr_number",
            "appointment.preferred_cities",
            "appointment.date_range.start_date",
            "appointment.date_range.end_date",
            "monitoring.check_interval",
            "monitoring.retry_interval",
            "browser.type",
        ]

        for field in required_fields:
            parts = field.split(".")
            current = config
            for part in parts:
                if not isinstance(current, dict) or part not in current:
                    raise ValueError(f"Missing required configuration field: {field}")
                current = current[part]

        return True
