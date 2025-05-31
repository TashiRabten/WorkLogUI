# WorkLogUI
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/f23bd6a8b8564852974cc0ee7f39249f)](https://app.codacy.com/gh/TashiRabten/WorkLogUI/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Version](https://img.shields.io/badge/version-1.1.9-blue.svg)](https://github.com/TashiRabten/WorkLogUI/releases)
[![JavaFX](https://img.shields.io/badge/JavaFX-24.0.1-orange.svg)](https://openjfx.io/)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS-lightgrey.svg)](https://github.com/TashiRabten/WorkLogUI/releases)

A comprehensive JavaFX-based time tracking and billing management application designed for self-employed professionals and independent contractors. Features advanced tax calculations, AGI (Adjusted Gross Income) reporting, and SSDI-compliant earnings monitoring for disability benefits recipients. **Now available for both Windows and macOS!**

---

## 🌟 Features

### 🕐 **Advanced Time Tracking**
- Log work hours and minutes for multiple companies with precision
- Support for both hourly and per-minute rate calculations
- Double pay option for overtime, holidays, or premium work
- Real-time earnings calculation with live rate updates
- Visual earnings tracking with intelligent monthly warnings

### 💼 **Company & Rate Management**
- Comprehensive company profile management (add, edit, delete)
- Flexible rate structures (hourly or per-minute billing)
- Real-time rate updates that immediately affect all calculations
- Track earnings performance by company over time
- Support for multiple concurrent clients

### 💸 **Advanced Bill Management & Tax Categorization**
- **NEW**: Smart expense categorization system with 20+ business categories
- Track deductible vs. non-deductible expenses automatically
- Home office percentage calculations (9% default, customizable)
- Monthly bill tracking with comprehensive filtering options
- Integrated bill editor with full CRUD operations
- Calculate net earnings after business expenses

### 📊 **Professional Tax Reporting & AGI Calculations**
- **NEW**: Complete AGI (Adjusted Gross Income) calculations
- **NEW**: NESE (Net Earnings from Self-Employment) computation
- **NEW**: Self-Employment Tax calculations (15.3% rate)
- **NEW**: SE Tax deduction calculations (50% deductible)
- Export to Excel with detailed tax-ready formatting
- Monthly and yearly financial summaries
- Filter data by date range, company, or expense category
- Real-time net total calculations

### 🚨 **SSDI Compliance & SGA Monitoring**
- **NEW**: Automatic SGA (Substantial Gainful Activity) limit monitoring
- **NEW**: Real-time warnings when approaching monthly NESE limits
- **NEW**: Multi-level alert system (90%, 100%, 110%, 120%, 130%+ of SGA)
- **NEW**: Monthly SSA-countable income calculations
- **NEW**: Bilingual alerts (English/Portuguese) for accessibility
- **NEW**: Smart popup warnings for filtered months and current earnings

### 🔄 **Auto-Update & Data Management**
- Automatic update checks at startup with seamless installation
- Manual update option available in the application interface
- **NEW**: Intelligent data migration system for legacy bill formats
- **NEW**: Monthly file structure for improved performance
- **NEW**: Automatic category assignment for existing expenses
- Backup protection with automatic file versioning
- Robust data validation and error recovery

---

## 🖥️ Installation

### Requirements
- **Operating System**:
    - **Windows**: Windows 10 or later
    - **macOS**: macOS 10.15 (Catalina) or later
- **Java Runtime**: Java 21+ (bundled with installer - no separate installation needed)
- **Memory**: 256MB RAM minimum, 512MB recommended
- **Storage**: 50MB for application, additional space for log files

### Download
Download the latest release from the [releases page](https://github.com/TashiRabten/WorkLogUI/releases):
- **Windows**: `worklog-setup-[version].exe`
- **macOS**: `WorkLog-[version].pkg`

### Platform-Specific Installation

#### Windows Installation
1. Download `worklog-setup-[version].exe`
2. Run the installer (may require administrator privileges)
3. Follow the installation wizard
4. Launch from Start Menu or Desktop shortcut

#### macOS Installation
1. Download `WorkLog-[version].pkg`
2. Double-click to open the installer
3. Follow the installation wizard
4. Launch from Applications folder or Launchpad
5. **Note**: On first launch, you may need to allow the app in System Preferences > Security & Privacy

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

---

## 🚀 Quick Start Guide

### First Time Setup
1. Launch the application
2. Add companies via "✎ Edit Companies" button
3. Set hourly or per-minute rates for each company
4. Configure expense categories in "💸 Edit Bills"

### Logging Work
1. Select or enter the date (MM/DD/YYYY format)
2. Choose a company from the dropdown
3. Enter hours or minutes worked (based on company rate type)
4. Check "Double Pay" for overtime/holidays/premium work
5. Click "Log Work"

### Managing Bills & Expense Categories
1. Click "💸 Edit Bills" button
2. **NEW**: Choose from 20+ expense categories:
    - **Deductible**: Office Rent, Utilities, Equipment, Business Mileage, etc.
    - **Non-deductible**: Personal expenses, entertainment, etc.
    - **Home Office**: Automatically applies 9% business use percentage
3. Add expenses with date, description, amount, and category
4. Mark bills as paid/unpaid for cash flow tracking
5. Bills automatically calculate tax deductions and AGI impact

### NEW: Understanding Your Tax Summary
The application now provides comprehensive tax calculations:
- **Gross Income**: Total earnings from all work
- **Business Expenses**: Deductible expenses (reduces taxable income)
- **Net Earnings**: Gross income minus business expenses
- **NESE**: Net Earnings × 92.35% (for Social Security calculations)
- **Self-Employment Tax**: NESE × 15.3%
- **SE Tax Deduction**: SE Tax × 50% (deductible on Form 1040)
- **AGI**: Net Earnings minus SE Tax Deduction
- **Monthly SSA Countable**: For SGA compliance monitoring

### NEW: SSDI/SGA Compliance Features
For users receiving disability benefits:
1. **Automatic SGA Monitoring**: Tracks monthly NESE against current year limits
    - 2025: $1,620/month
    - 2024: $1,550/month
    - 2023: $1,470/month
2. **Smart Warnings**:
    - Green: Approaching 90% of limit
    - Yellow: 90-100% of limit
    - Orange: 100-120% of limit
    - Red: Over 120% of limit
3. **Bilingual Alerts**: English and Portuguese support
4. **Real-time Monitoring**: Updates as you log work throughout the month

### Viewing Reports & Analytics
- **Show Time**: View total hours worked per company
- **Show Earnings**: Display earnings breakdown by company
- **Monthly/Yearly Summary**: Comprehensive financial overview with tax calculations
- **Export Excel**: Generate detailed Excel reports with:
    - Complete AGI calculations
    - Business expense categorization
    - Tax-ready formatting
    - SSA countable income
    - Cash flow analysis (income after all bills)

### Filtering Data
- Use Year/Month/Company dropdowns to filter displayed data
- Apply filters to view specific time periods or companies
- Clear filters to show all data
- **NEW**: Filter-specific SGA warnings for historical analysis

---

## 📁 Data Storage & Security

### Data Storage
All data is stored locally for privacy and security:

#### Windows
- **Location**: `C:\Users\[username]\Documents\WorkLog\`

#### macOS
- **Location**: `/Users/[username]/Documents/WorkLog/`

### File Structure (Both Platforms)
- `logs/`: Monthly work log files organized by year-month (e.g., `2025-01.json`)
- `bills/`: Monthly bill records organized by year-month (e.g., `2025-01.json`)
- `company-rates.json`: Company configurations and rates
- `exports/`: Excel export files with timestamps
- `installer/`: Auto-updater downloads (temporary)

### NEW: Monthly File Architecture
**Version 1.1.9** introduces an improved file structure:
- Work logs are now stored in separate monthly files for better performance
- Automatic migration from legacy `worklog.json` format
- Faster loading and filtering with large datasets
- Improved data integrity and backup capabilities

### NEW: SGA Warning System
The application includes an intelligent warning system specifically designed for SSDI recipients:

**Warning Levels**:
- **90-100% of SGA limit**: Green advisory warnings
- **At 100%**: Exact limit notifications
- **100-110%**: Yellow caution warnings
- **110-120%**: Orange serious warnings
- **120-130%**: Red critical warnings
- **Over 130%**: Emergency red alerts

**Smart Features**:
- Startup warnings for current month earnings
- Filter-specific warnings when viewing historical data
- Popup alerts for serious violations (over 10% of limit)
- Bilingual support for accessibility
- Automatic SGA limit updates for each tax year

---

## 🛠️ Technical Details

### Technologies & Architecture
- **JavaFX 24.0.1**: Modern, responsive UI framework with native platform integration
- **Java 21**: Latest LTS version with enhanced performance and cross-platform compatibility
- **Maven**: Dependency management and build system
- **Apache POI**: Excel export with advanced formatting
- **Jackson**: Robust JSON processing for data persistence
- **MVC Architecture**: Clean separation of concerns
- **Service Layer**: Business logic isolation
- **Observer Pattern**: Reactive UI updates
- **Singleton Services**: Efficient resource management

### Cross-Platform Compatibility
- **Native Look & Feel**: Adapts to platform-specific UI guidelines
- **File System Integration**: Proper path handling for Windows and macOS
- **System Tray Integration**: Platform-appropriate system integration
- **Keyboard Shortcuts**: Platform-specific key combinations (Cmd on Mac, Ctrl on Windows)

### NEW: Tax Calculation Engine
- **AGI Calculator**: Comprehensive tax calculations following IRS guidelines
- **NESE Calculations**: Accurate Social Security earnings computation
- **Expense Categorization**: 20+ business expense categories
- **Home Office Deductions**: Automated percentage calculations
- **Multi-year SGA Limits**: Built-in knowledge of changing SGA thresholds

### Supported Expense Categories
**Deductible Business Expenses:**
- Office Rent, Utilities, Office Supplies
- Equipment/Depreciation, Business Mileage
- Business Travel, Business Insurance
- Professional Services, Advertising/Marketing
- Phone/Internet, Repairs & Maintenance
- Contractor Payments, Professional Education
- Bank/Merchant Fees, Business Supplies
- Health Insurance Premium, Home Office

**Non-Deductible:**
- Personal expenses, Entertainment (limited deductibility)

---

## 🔄 Auto-Updates

### Cross-Platform Update System
- ✅ Automatic update checking on startup (Windows & macOS)
- 🔄 Manual update check via the update button in interface
- 📥 Platform-specific installer download:
    - Windows: `.exe` installer with Windows Installer technology
    - macOS: `.pkg` installer with native macOS installation
- 🔄 Seamless version upgrades without data loss
- 💾 Automatic backup creation before major updates
- 🔐 Code-signed installers for security (macOS Gatekeeper compatible)

---

## 🐛 Troubleshooting

### Common Issues & Solutions

#### Cross-Platform Issues

**Application doesn't start**
- **Windows**: Ensure Java 21+ is installed and accessible
- **macOS**: Allow the app in System Preferences > Security & Privacy if blocked
- Check if data files are corrupted (delete WorkLog folder in Documents to recreate)
- Verify write permissions in Documents/WorkLog directory

**macOS-Specific Issues**
- **"WorkLog is damaged and can't be opened"**: Right-click and select "Open" to bypass Gatekeeper
- **Permission denied errors**: Grant Full Disk Access in System Preferences > Security & Privacy > Privacy
- **App crashes on startup**: Check Console.app for detailed error logs

**Windows-Specific Issues**
- **Installer fails**: Run as Administrator or temporarily disable antivirus
- **Missing DLL errors**: Ensure Windows is up to date
- **Performance issues**: Check Windows Defender isn't scanning the WorkLog folder repeatedly

#### Data Issues

**SGA warnings not appearing**
- Verify your earnings data is current and properly categorized
- Check that bills are marked with correct deductible/non-deductible categories
- Ensure date filters are set to current month for live monitoring

**Excel export fails**
- Check available disk space (exports can be large with extensive data)
- Ensure export folder exists and has write permissions
- Try exporting smaller date ranges if file size is excessive
- **macOS**: Verify Excel isn't running and blocking file access

**Data migration issues (upgrading to 1.1.9)**
- Backup your Documents/WorkLog folder before upgrading
- Old `worklog.json` files are automatically migrated to monthly structure
- If migration fails, the app will prompt to restore from backup

---

## 📚 Use Cases

- **Freelance Professionals**: Track multiple clients and projects with detailed financial reporting
- **Independent Contractors**: Monitor earnings for tax preparation and quarterly filing
- **SSDI Recipients**: Ensure SGA compliance while maximizing allowable earnings
- **Disability Advocates**: Help clients track earnings within benefit guidelines
- **Small Business Owners**: Simple time tracking with professional tax reporting
- **Tax Professionals**: Provide clients with detailed, categorized expense tracking
- **Cross-Platform Teams**: Consistent experience across Windows and macOS environments

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for:
- 🐛 Bug reports and feature requests
- 🔧 Code contributions and pull requests
- 🌍 Translations and localization improvements
- 📖 Documentation enhancements
- 🖥️ Platform-specific testing (Windows/macOS)

### Code Style & Guidelines
- Follow Java naming conventions and best practices
- Use meaningful variable and method names that clearly express intent
- Add comprehensive comments for complex business logic (especially tax calculations)
- Maintain bilingual support (English/Portuguese) for all user-facing text
- Include unit tests for new financial calculation features
- Document any changes to AGI or SGA calculation logic
- Test on both Windows and macOS platforms before submitting PRs

### Priority Areas for Contribution
- **Linux Support**: Native packaging for Linux distributions
- **Internationalization**: Additional language support beyond English/Portuguese
- **Tax Features**: Support for additional countries' tax systems
- **Accessibility**: Enhanced support for screen readers and keyboard navigation
- **API Integration**: Connect with popular accounting software
- **Backup/Sync**: Cloud backup and multi-device synchronization

---

## 📜 License

MIT License - This software is open-source and free to use, modify, and distribute.

---

## 🙏 Acknowledgments

- **JavaFX Community**: For the excellent cross-platform UI framework
- **Apache POI Team**: For comprehensive Excel functionality
- **Jackson Team**: For robust JSON processing capabilities
- **SSDI Community**: For feedback on SGA compliance features
- **Beta Testers**: For extensive testing of tax calculation accuracy across platforms
- **Disability Rights Advocates**: For guidance on accessibility features
- **Contributors and Translators**: For expanding language support
- **macOS Beta Testers**: For helping perfect the macOS experience

---

## 📫 Support & Documentation

For comprehensive support and detailed documentation:
- **Issues & Bug Reports**: [GitHub Issues](https://github.com/TashiRabten/WorkLogUI/issues)
- **Feature Requests**: Use the "enhancement" label on GitHub Issues
- **Platform-Specific Issues**: Tag issues with "Windows" or "macOS" labels
- **Tax Calculation Questions**: See the built-in help tooltips and Excel export documentation
- **SGA Compliance**: Refer to official SSA guidelines; this tool is for tracking only
- **Developer Documentation**: Check the [Wiki](https://github.com/TashiRabten/WorkLogUI/wiki) for detailed guides

**Important Disclaimer**: This application is designed to help track earnings and expenses for tax and SGA compliance purposes. It is not a substitute for professional tax or legal advice. Users are responsible for verifying calculations and ensuring compliance with their specific tax and disability benefit requirements.

---

**Note**: WorkLogUI is designed specifically for self-employed professionals, independent contractors, and individuals receiving disability benefits who need precise earnings tracking for SGA compliance. The advanced tax calculation features make it particularly suitable for users who need detailed financial reporting for tax preparation or disability benefit monitoring. **Now available on both Windows and macOS for maximum flexibility.**

---

*Last updated: January 2025 | Version 1.1.9+*