# WorkLogUI
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/YOUR_CODACY_PROJECT_ID)](https://www.codacy.com/gh/TashiRabten/WorkLogUI/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TashiRabten/WorkLogUI&amp;utm_campaign=Badge_Grade)
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/TashiRabten/WorkLogUI/releases)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS-lightgrey.svg)](https://github.com/TashiRabten/WorkLogUI)
[![JavaFX](https://img.shields.io/badge/JavaFX-24.0.1-orange.svg)](https://openjfx.io/)

A JavaFX-based time tracking and billing management application that helps you log work hours, manage company rates, track bills, and generate comprehensive financial reports.

## Features

### 🕐 Time Tracking
- Log work hours and minutes for multiple companies
- Support for hourly and per-minute rate calculations
- Double pay option for overtime or holidays
- Visual earnings tracking with monthly warnings ($1,600 threshold)

### 💼 Company Management
- Add, edit, and delete company profiles
- Set custom rates (hourly or per-minute)
- Track earnings per company
- Real-time rate updates affect calculations immediately

### 💸 Bill Management
- Track monthly expenses and bills
- Filter bills by year, month, or company
- Integrated bill editor with full CRUD operations
- Calculate net earnings (gross earnings - bills)

### 📊 Reporting & Export
- Export to Excel with detailed formatting
- Monthly and yearly summaries
- Filter data by date range or company
- Real-time net total calculations

### 🔄 Auto-Update System
- Automatic update checks at startup
- Manual update option in the application
- Seamless upgrade process for Windows and macOS

## Installation

### Requirements
- Java 21 or higher
- JavaFX 24.0.1 (included in application bundle)

### Download
Download the latest release from the [releases page](https://github.com/TashiRabten/WorkLogUI/releases):
- **Windows**: `worklog-setup.exe`

### Build from Source
```bash
# Clone the repository
git clone https://github.com/TashiRabten/WorkLogUI.git
cd WorkLogUI

# Build with Maven
mvn clean package

# Run the application
mvn javafx:run
```

## Usage

### First Time Setup
1. Launch the application
2. Add companies via "Edit Companies" button
3. Set hourly or per-minute rates for each company

### Logging Work
1. Enter the date (MM/DD/YYYY format)
2. Select a company from the dropdown
3. Enter hours or minutes worked
4. Check "Double Pay" for overtime/holidays
5. Click "Log Work"

### Managing Bills
1. Click "Edit Bills" button
2. Add monthly expenses with date, description, and amount
3. Mark bills as paid/unpaid
4. Bills automatically deduct from gross earnings

### Viewing Reports
- **Show Time**: View total hours worked per company
- **Show Earnings**: Display earnings breakdown by company
- **Monthly/Yearly Summary**: See comprehensive financial overview
- **Export Excel**: Generate detailed Excel reports with earnings and bills

### Filtering Data
- Use Year/Month/Company dropdowns to filter displayed data
- Apply filters to view specific time periods or companies
- Clear filters to show all data

## Configuration

### Data Storage
All data is stored locally in your Documents folder:
- Windows: `C:\Users\[username]\Documents\WorkLog\`
- macOS: `~/Documents/WorkLog/`

Files:
- `worklog.json`: Time tracking entries
- `company-rates.json`: Company configurations
- `bills/`: Monthly bill records
- `exports/`: Excel export files

### Monthly Warning System
The application includes a smart warning system that alerts when:
- Approaching $1,600 monthly limit (90-100%)
- Exceeding the limit (shows percentage over)
- Different severity levels with color-coded warnings

## Technical Details

### Technologies
- JavaFX 24.0.1
- Java 21
- Maven build system
- Apache POI for Excel exports
- Jackson for JSON processing

### Architecture
- MVC pattern with FXML views
- Service-based business logic
- Singleton pattern for shared services
- Observer pattern for UI updates

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Create a Pull Request

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Maintain bilingual support (English/Portuguese)

## Troubleshooting

### Common Issues

**Application doesn't start**
- Ensure Java 21+ is installed
- Check if `worklog.json` is corrupted (delete and restart)

**Update fails**
- Check internet connection
- Manually download from releases page
- Ensure write permissions in application directory

**Excel export fails**
- Check disk space
- Ensure export folder exists and has write permissions
- Try exporting fewer records

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- JavaFX community for the excellent UI framework
- Apache POI for Excel functionality
- Contributors and testers

## Support

For issues, questions, or suggestions:
- Open an issue on [GitHub](https://github.com/TashiRabten/WorkLogUI/issues)
- Check existing issues for solutions

---

**Note**: Replace `YOUR_CODACY_PROJECT_ID` in the Codacy badge with your actual Codacy project ID after setting up Codacy integration.
