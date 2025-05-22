# US Visa Appointment Scheduler

An automated tool for monitoring and booking US visa appointments, built with Spring Boot.

## Features

- Multi-browser support (Chrome, Firefox, Edge)
- Email notifications for appointment availability
- Configurable appointment preferences
- Auto-booking capability
- Detailed logging
- Spring Boot architecture with async support
- Multi-worker capability (future expansion)

## Project Structure

```
.
├── src/main/java/com/easyslot/
│   ├── browser/                # Browser driver management
│   ├── config/                 # Configuration handling
│   │   ├── model/             # Configuration models
│   │   ├── AppConfiguration.java  # Spring config class
│   │   └── MultiUserConfigLoader.java # Multi-user support
│   ├── model/                  # Data models
│   ├── notification/           # Email notification system
│   ├── scheduler/              # Scheduler implementation
│   │   ├── AppointmentScheduler.java       # Core scheduling logic
│   │   └── AppointmentSchedulerStarter.java # Worker starter
│   ├── state/                  # State management
│   └── EasySlotApplication.java # Spring Boot entry point
└── src/main/resources/
    └── application.properties  # Spring Boot configuration
```

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- Chrome, Firefox, or Edge browser

## Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/easy-slot.git
cd easy-slot
```

2. Build the project:
```bash
mvn clean package
```

3. Configure the application:
```bash
cp config.example.yml config.yml
```
Edit `config.yml` with your:
- Visa account credentials
- Gmail app password for notifications
- Preferred appointment locations and dates
- Browser preferences

## Usage

### Running with Scripts

The project includes convenient deployment scripts for both Linux and macOS:

#### Linux
```bash
./deploy_linux.sh
```

#### macOS
```bash
./deploy_macos.sh
```

These scripts will:
1. Check and install necessary dependencies
2. Build the project
3. Provide options to run in foreground or background mode

### Running Manually

To run the application manually:

```bash
java -jar target/easy-slot-1.0-SNAPSHOT.jar
```

## Configuration

### YAML Configuration
The `config.yml` file contains visa account and appointment settings:

```yaml
credentials:
  email: "your-email@example.com"
  password: "your-password"

appointment:
  ivrNumber: "88888888"  # Your IVR Account Number
  preferredCities:
    - "Toronto"
    - "Vancouver"
    - "Calgary"
  
  dateRange:
    startDate: "2024-03-20"  # Earliest acceptable date
    endDate: "2024-12-31"    # Latest acceptable date
  
  autoBook: false  # Set to true for automatic booking

monitoring:
  checkInterval: 300  # Time between checks in seconds
  retryInterval: 60   # Time to wait after errors in seconds

notifications:
  gmail:
    enabled: true
    email: "your-gmail@gmail.com"
    appPassword: "your-16-digit-app-password"
    recipientEmail: "recipient@email.com"

browser:
  type: "chrome"  # Supported: chrome, firefox, edge
  headless: true  # Run browser in headless mode
  options:
    chrome:
      useExisting: false
      binaryLocation: ""  # Optional: Path to Chrome binary
```

### Spring Boot Configuration
Application settings are in `src/main/resources/application.properties`:

```properties
# Application configuration path
config.path=config.yml

# Server port (if web UI is enabled)
server.port=8080

# Logging configuration
logging.level.root=INFO
logging.level.com.easyslot=DEBUG
logging.file.name=logs/application.log
```

## Browser Support

Supported browsers:
- Chrome (with ChromeDriver)
- Firefox (with GeckoDriver)
- Edge (with EdgeDriver)

## Multi-Worker Support

The application has been designed to support multiple workers in the future:

- Each worker can use different visa accounts and settings
- Workers run in separate threads via Spring's `@Async` support
- Each worker's state is managed independently
- Support for loading multiple configurations

## License

This project is licensed under the MIT License - see the LICENSE file for details. 