package com.example.worklogui;
import java.util.Map;

public class WorkLogger {

    // Updates the existing log entry by parsing and modifying the stored values
    public static void updateLogEntry(Map<String, WorkLogEntry> logEntries, String date, String jobType, int value) {
        if (!logEntries.containsKey(date)) {
            System.out.println("Date not found in log entries.");
            return;
        }

        WorkLogEntry entry = logEntries.get(date);

        if (jobType.toLowerCase().contains("sosi")) {
            entry.addSosiHours(value);
        } else if (jobType.toLowerCase().contains("lion") || jobType.toLowerCase().contains("bridge")) {
            entry.addLionBridgeMinutes(value);
        }

        logEntries.put(date, entry);
    }
}
