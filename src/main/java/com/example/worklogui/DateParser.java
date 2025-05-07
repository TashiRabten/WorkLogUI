package com.example.worklogui;

import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;


// Date parsing logic extracted
public class DateParser {
    private static final DateTimeFormatter SHORT_FORMATTER = DateTimeFormatter.ofPattern("M/d/yy");
    private static final DateTimeFormatter LONG_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    public static LocalDate parseDate(String dateString) throws DateTimeParseException {
        dateString = dateString.trim();

        if (dateString.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            return LocalDate.parse(dateString, LONG_FORMATTER.withLocale(Locale.US));
        } else if (dateString.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
            return LocalDate.parse(dateString, SHORT_FORMATTER.withLocale(Locale.US));
        }

        throw new DateTimeParseException("Invalid date format \n ❌ Erro de formato de data", dateString, 0);
    }

    public static LocalDate parseLineDate(String datePart) {
        try {
            DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('/')
                    .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH)
                    .appendLiteral('/')
                    .appendValueReduced(
                            java.time.temporal.ChronoField.YEAR,
                            2, 2,
                            2000  // assume base year 2000 for 'yy' format
                    )
                    .toFormatter(Locale.US)
                    .withChronology(IsoChronology.INSTANCE)
                    .withResolverStyle(ResolverStyle.STRICT);

            return LocalDate.parse(datePart, dateFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
