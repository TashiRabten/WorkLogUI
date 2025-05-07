package com.example.worklogui;

import java.nio.file.Path;
import java.util.Locale;

public class AppConstants {

    // App title (for windows/dialogs)
    public static final String APP_TITLE = "WorkLog";

    // File paths
    public static final Path WORKLOG_PATH = Path.of("worklog.json");
    public static final Path RATES_PATH = Path.of("company-rates.json");

    // Backup paths (optional safety backups)
    public static final Path BACKUP_WORKLOG_PATH = Path.of("backup", "worklog-backup.json");
    public static final Path BACKUP_RATES_PATH = Path.of("backup", "rates-backup.json");

    // Date format (US style)
    public static final String DATE_FORMAT = "MM/dd/yyyy";

    // Fixed locale (for US users)
    public static final Locale APP_LOCALE = Locale.US;

    // Currency symbol
    public static final String CURRENCY_SYMBOL = "$";

    // Error messages (bilingual)
    public static final String ERROR_INVALID_RATE_EN = "Invalid rate value.";
    public static final String ERROR_INVALID_RATE_PT = "Valor de pagamento inválido.";
}
