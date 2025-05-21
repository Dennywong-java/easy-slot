# US Visa Appointment Scheduler

An automated tool for monitoring and booking US visa appointments.

## Features

- Multi-browser support (Chrome, Firefox, Edge, Safari)
- Email notifications for appointment availability
- Configurable appointment preferences
- Auto-booking capability
- Detailed logging

## Project Structure

```
src/
├── browser/            # Browser driver management
├── config/             # Configuration handling
├── notification/       # Email notification system
├── utils/             # Utility functions
└── scheduler.py       # Main scheduler implementation
```

## Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/easy-slot.git
cd easy-slot
```

2. Create and activate a virtual environment:
```bash
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Configure the application:
```bash
cp config.example.yml config.yml
```
Edit `config.yml` with your:
- Visa account credentials
- Gmail app password for notifications
- Preferred appointment locations and dates
- Browser preferences

## Usage

Run the scheduler:
```bash
python -m src
```

## Configuration

The `config.yml` file contains all necessary settings:

- `credentials`: Your visa account login details
- `appointment`: Appointment preferences and IVR number
- `monitoring`: Check intervals and retry settings
- `notifications`: Email notification settings
- `browser`: Browser selection and options

## Browser Support

Supported browsers:
- Chrome (with ChromeDriver)
- Firefox (with GeckoDriver)
- Edge (with EdgeDriver)
- Safari (built-in WebDriver)

## License

This project is licensed under the MIT License - see the LICENSE file for details. 