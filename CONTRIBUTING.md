# Contributing to WorkLogUI

Thank you for your interest in contributing to WorkLogUI! This guide will help you get started with contributing to our JavaFX-based time tracking and billing management application.

## 🎯 Project Overview

WorkLogUI is designed for self-employed professionals, independent contractors, and SSDI recipients who need precise earnings tracking for tax compliance and SGA (Substantial Gainful Activity) monitoring. Our focus is on accuracy, reliability, and accessibility.

## 🚀 Getting Started

### Prerequisites

- **Java 21+** (LTS version required)
- **JavaFX 24.0.1+** (handled by Maven dependencies)
- **Maven 3.8+** for build management
- **Git** for version control
- **IDE** recommendation: IntelliJ IDEA or Eclipse with JavaFX support

### Setting Up Development Environment

1. **Fork and Clone**
   ```bash
   git clone https://github.com/[your-username]/WorkLogUI.git
   cd WorkLogUI
   ```

2. **Install Dependencies**
   ```bash
   mvn clean install
   ```

3. **Run the Application**
   ```bash
   mvn javafx:run
   ```

4. **Verify Setup**
   - Application should launch without errors
   - Test basic functionality (add company, log work, add bill)
   - Check that test data appears correctly

### Project Structure

```
WorkLogUI/
├── src/main/java/
│   └── com/worklogui/
│       ├── controller/          # JavaFX Controllers (UI logic)
│       ├── model/              # Data models and entities
│       ├── service/            # Business logic and data services
│       ├── util/               # Utility classes and helpers
│       └── WorkLogApp.java     # Main application entry point
├── src/main/resources/
│   ├── fxml/                   # JavaFX FXML files
│   ├── css/                    # Stylesheets
│   └── images/                 # Application icons and images
├── src/test/java/              # Unit and integration tests
└── target/                     # Build output (generated)
```

## 🐛 How to Contribute

### Reporting Bugs

Before submitting a bug report:
1. **Search existing issues** to avoid duplicates
2. **Test with the latest version** to ensure the bug still exists
3. **Reproduce the bug** with minimal steps

**Bug Report Template:**
```markdown
**Bug Description:**
Clear description of what went wrong

**Steps to Reproduce:**
1. Go to '...'
2. Click on '...'
3. Enter '...'
4. See error

**Expected Behavior:**
What should have happened

**Actual Behavior:**
What actually happened

**Environment:**
- OS: [Windows 10/11]
- Java Version: [java -version output]
- WorkLogUI Version: [from Help > About]

**Additional Context:**
- Screenshots if applicable
- Error logs from Documents/WorkLog/ folder
- Any recent changes to data files
```

### Suggesting Features

**Feature Request Template:**
```markdown
**Feature Summary:**
Brief description of the proposed feature

**Problem/Use Case:**
What problem does this solve? Who would benefit?

**Proposed Solution:**
Detailed description of how the feature should work

**Alternatives Considered:**
Other approaches you've thought about

**Priority Level:**
- Critical (blocks core functionality)
- High (significantly improves user experience)
- Medium (nice to have enhancement)
- Low (minor improvement)

**Target Users:**
- [ ] General freelancers
- [ ] SSDI recipients
- [ ] Tax professionals
- [ ] All users
```

### Contributing Code

#### Branch Naming Convention
- `feature/short-description` - New features
- `bugfix/issue-number-description` - Bug fixes
- `hotfix/critical-issue` - Critical production fixes
- `docs/section-name` - Documentation updates
- `refactor/component-name` - Code refactoring

#### Pull Request Process

1. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Your Changes**
   - Follow our coding standards (see below)
   - Add tests for new functionality
   - Update documentation as needed

3. **Test Thoroughly**
   ```bash
   # Run all tests
   mvn test
   
   # Test the application manually
   mvn javafx:run
   ```

4. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat: add SGA warning customization
   
   - Allow users to set custom SGA warning thresholds
   - Add validation for percentage inputs
   - Update UI to show current threshold settings
   
   Fixes #123"
   ```

5. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   ```

#### Commit Message Format

Use conventional commits format:
```
type(scope): short description

Longer description if needed

- Bullet point details
- Additional context
- Reference issues: Fixes #123, Closes #456
```

**Types:**
- `feat:` - New features
- `fix:` - Bug fixes
- `docs:` - Documentation changes
- `style:` - Code formatting (no logic changes)
- `refactor:` - Code restructuring
- `test:` - Adding or updating tests
- `chore:` - Build/dependency updates

## 📝 Code Standards

### Java Code Style

**Naming Conventions:**
```java
// Classes: PascalCase
public class TaxCalculationService { }

// Methods and variables: camelCase
private double calculateNetEarnings() { }
private String companyName;

// Constants: UPPER_SNAKE_CASE
private static final double SGA_LIMIT_2025 = 1620.0;

// Packages: lowercase with dots
package com.worklogui.service.tax;
```

**Method Documentation:**
```java
/**
 * Calculates Net Earnings from Self-Employment (NESE) for Social Security purposes.
 * NESE = Net Earnings × 92.35% (as per IRS Schedule SE)
 * 
 * @param netEarnings Total earnings after business expenses
 * @return NESE amount for Social Security calculations
 * @throws IllegalArgumentException if netEarnings is negative
 * @since 1.1.8
 */
public double calculateNESE(double netEarnings) {
    if (netEarnings < 0) {
        throw new IllegalArgumentException("Net earnings cannot be negative");
    }
    return netEarnings * 0.9235; // 92.35% as per IRS guidelines
}
```

**Error Handling:**
```java
// Prefer specific exceptions with meaningful messages
try {
    double result = performCalculation(value);
} catch (NumberFormatException e) {
    logger.error("Invalid numeric input for tax calculation: " + value, e);
    showUserError("Please enter a valid number for the amount");
    return;
} catch (Exception e) {
    logger.error("Unexpected error during calculation", e);
    showUserError("An error occurred. Please check your input and try again.");
    return;
}
```

### JavaFX Best Practices

**Controller Structure:**
```java
@FXML
public class MainController {
    @FXML private TextField hoursWorked;
    @FXML private ComboBox<Company> companyComboBox;
    @FXML private Label totalEarnings;
    
    private WorkLogService workLogService;
    private TaxCalculationService taxService;
    
    @FXML
    private void initialize() {
        // Initialize services
        // Set up bindings
        // Configure UI components
    }
    
    @FXML
    private void handleLogWork(ActionEvent event) {
        // Event handler implementation
    }
}
```

**UI Updates:**
```java
// Always update UI on JavaFX Application Thread
Platform.runLater(() -> {
    totalEarningsLabel.setText(formatCurrency(newTotal));
    updateSGAWarningDisplay(currentMonthEarnings);
});
```

### Financial Calculation Guidelines

**Precision and Rounding:**
```java
// Use BigDecimal for financial calculations
public BigDecimal calculateSelfEmploymentTax(BigDecimal nese) {
    BigDecimal rate = new BigDecimal("0.153"); // 15.3%
    return nese.multiply(rate).setScale(2, RoundingMode.HALF_UP);
}

// Format currency consistently
private String formatCurrency(double amount) {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
}
```

**Tax Calculation Accuracy:**
```java
// Document all tax calculations with IRS references
/**
 * Calculates SE Tax deduction per IRS Form 1040 instructions.
 * Deduction = Self-Employment Tax × 50%
 * Reference: IRS Publication 334, Self-Employment Tax
 */
public double calculateSETaxDeduction(double seTax) {
    return seTax * 0.5; // Exactly 50% per IRS guidelines
}
```

## 🧪 Testing Guidelines

### Unit Tests

```java
@Test
public void testSGAComplianceWarning() {
    // Arrange
    double monthlyEarnings = 1500.0;
    double sgaLimit = 1620.0;
    
    // Act
    SGAWarningLevel level = sgaService.getWarningLevel(monthlyEarnings, sgaLimit);
    
    // Assert
    assertEquals(SGAWarningLevel.APPROACHING, level);
    assertTrue("Should show warning when over 90%", 
               monthlyEarnings / sgaLimit > 0.9);
}

@Test
public void testTaxCalculationAccuracy() {
    // Test with known values
    BigDecimal netEarnings = new BigDecimal("50000.00");
    BigDecimal expectedNESE = new BigDecimal("46175.00"); // 50000 * 0.9235
    
    BigDecimal actualNESE = taxCalculator.calculateNESE(netEarnings);
    
    assertEquals("NESE calculation must be precise", expectedNESE, actualNESE);
}
```

### Integration Tests

```java
@Test
public void testCompleteWorkflowIntegration() {
    // Test complete user workflow
    // 1. Add company
    // 2. Log work
    // 3. Add bills
    // 4. Generate tax report
    // 5. Verify SGA compliance
}
```

### Manual Testing Checklist

Before submitting a PR, test:
- [ ] Application launches without errors
- [ ] Core functionality (log work, add bills, generate reports)
- [ ] SGA warnings appear at correct thresholds
- [ ] Tax calculations match expected values
- [ ] Excel export works and contains correct data
- [ ] UI remains responsive during operations
- [ ] Data persists correctly between sessions
- [ ] Error messages are user-friendly
- [ ] Bilingual text displays correctly (English/Portuguese)

## 🌍 Internationalization (i18n)

### Adding New Languages

1. **Create Resource Bundle**
   ```
   src/main/resources/i18n/
   ├── messages.properties (English - default)
   ├── messages_pt.properties (Portuguese)
   └── messages_[language_code].properties (New language)
   ```

2. **Resource Bundle Format**
   ```properties
   # messages_es.properties (Spanish example)
   app.title=WorkLogUI - Seguimiento de Tiempo
   sga.warning.approaching=Acercándose al límite SGA
   tax.calculation.title=Cálculos de Impuestos
   ```

3. **Update Language Loading**
   ```java
   // In ResourceBundleUtil.java
   public static void setLocale(Locale locale) {
       currentBundle = ResourceBundle.getBundle("i18n.messages", locale);
   }
   ```

### Text Guidelines

- Keep messages concise but clear
- Use placeholder values: `"Total earnings: {0}"`
- Provide context for translators: `# This appears in the SGA warning dialog`
- Test all UI layouts with longer translations

## 🎨 UI/UX Guidelines

### Design Principles

1. **Accessibility First**
   - High contrast colors
   - Keyboard navigation support
   - Screen reader compatibility
   - Clear, readable fonts

2. **Professional Appearance**
   - Clean, business-appropriate design
   - Consistent spacing and alignment
   - Professional color scheme
   - Clear visual hierarchy

3. **User-Friendly**
   - Intuitive navigation
   - Clear error messages
   - Helpful tooltips and hints
   - Logical workflow progression

### CSS Standards

```css
/* Use consistent naming */
.primary-button {
    -fx-background-color: #2196F3;
    -fx-text-fill: white;
    -fx-font-weight: bold;
}

.warning-text {
    -fx-text-fill: #FF9800;
    -fx-font-weight: bold;
}

.error-text {
    -fx-text-fill: #F44336;
    -fx-font-weight: bold;
}
```

## 🔒 Security Considerations

### Data Protection

- **No sensitive data in logs** - Avoid logging personal information
- **Input validation** - Validate all user inputs
- **File permissions** - Ensure data files have appropriate permissions
- **Error messages** - Don't expose system details in user-facing errors

### Code Security

```java
// Input validation example
public boolean validateHours(String hoursInput) {
    try {
        double hours = Double.parseDouble(hoursInput);
        return hours >= 0 && hours <= 24; // Reasonable bounds
    } catch (NumberFormatException e) {
        return false;
    }
}

// Safe file operations
public void saveWorkLogData(WorkLogData data) {
    Path backupPath = createBackup();
    try {
        saveToFile(data);
    } catch (IOException e) {
        restoreFromBackup(backupPath);
        throw new DataSaveException("Failed to save work log data", e);
    }
}
```

## 📚 Documentation

### Code Documentation

- **JavaDoc for public APIs** - All public methods and classes
- **Inline comments for complex logic** - Especially tax calculations
- **README updates** - Update feature lists and usage instructions
- **Change documentation** - Update changelog for user-facing changes

### User Documentation

- **Feature descriptions** - Clear explanations of new features
- **Screenshots** - Visual guides for complex workflows
- **Examples** - Real-world usage scenarios
- **Troubleshooting** - Common issues and solutions

## 🚀 Release Process

### Version Numbering

We follow Semantic Versioning (SemVer):
- **Major** (1.x.x): Breaking changes, major redesigns
- **Minor** (x.1.x): New features, significant enhancements
- **Patch** (x.x.1): Bug fixes, minor improvements

### Release Checklist

- [ ] All tests passing
- [ ] Documentation updated
- [ ] Version numbers incremented
- [ ] Changelog updated
- [ ] Build artifacts tested
- [ ] Auto-updater functionality verified

## 🏆 Recognition

Contributors will be recognized in:
- **README acknowledgments** - Listed by contribution type
- **Release notes** - Credited for significant contributions
- **About dialog** - Contributors listed in application

## 💬 Communication

### Getting Help

- **GitHub Issues** - For bugs and feature requests
- **GitHub Discussions** - For questions and general discussion
- **Code Review** - Detailed feedback on pull requests

### Code of Conduct

We are committed to providing a welcoming and inclusive environment:
- **Be respectful** - Treat all contributors with kindness
- **Be constructive** - Provide helpful feedback
- **Be patient** - Help newcomers learn our processes
- **Be professional** - Maintain appropriate communication

## 🎯 Priority Contribution Areas

### High Priority
1. **Tax Calculation Accuracy** - Verify calculations against IRS publications
2. **SGA Compliance Features** - Enhance disability benefit monitoring
3. **Accessibility Improvements** - Screen reader support, keyboard navigation
4. **Data Migration** - Support for importing from other time tracking tools

### Medium Priority
1. **Additional Language Support** - Expand beyond English/Portuguese
2. **Advanced Reporting** - More detailed financial reports
3. **Performance Optimization** - Handle larger datasets efficiently
4. **Cloud Backup Integration** - Optional cloud storage for data

### Low Priority
1. **UI Enhancements** - Visual improvements and modern styling
2. **Advanced Filtering** - More sophisticated data filtering options
3. **Export Formats** - Additional export formats (PDF, CSV)
4. **Integration APIs** - Connect with accounting software

## 📋 Contribution Templates

### Feature Implementation Template

```java
/**
 * [Feature Name] Implementation
 * 
 * Purpose: [Brief description of what this feature does]
 * User Story: As a [user type], I want to [action] so that [benefit]
 * 
 * Implementation Notes:
 * - [Key implementation decisions]
 * - [Dependencies or requirements]
 * - [Testing approach]
 * 
 * @author [Your Name]
 * @since [Version]
 */
```

### Bug Fix Template

```java
/**
 * Bug Fix: [Issue Description]
 * 
 * Problem: [What was wrong]
 * Root Cause: [Why it happened]
 * Solution: [How it was fixed]
 * 
 * Fixes: #[issue-number]
 * 
 * @author [Your Name]
 */
```

## 🔧 Development Tools

### Recommended IDE Settings

**IntelliJ IDEA:**
- Enable JavaFX support
- Set up code formatting to match project style
- Configure checkstyle plugin with project rules
- Enable copyright headers for new files

**Eclipse:**
- Install e(fx)clipse plugin
- Configure code formatter
- Set up JavaFX build path

### Useful Maven Commands

```bash
# Full build with tests
mvn clean verify

# Run application
mvn javafx:run

# Generate JavaDoc
mvn javadoc:javadoc

# Check for dependency updates
mvn versions:display-dependency-updates

# Run specific test class
mvn test -Dtest=TaxCalculationServiceTest
```

---

Thank you for contributing to WorkLogUI! Your efforts help make financial tracking and tax compliance easier for self-employed professionals and SSDI recipients worldwide.

For questions about this contributing guide, please open an issue with the "documentation" label.