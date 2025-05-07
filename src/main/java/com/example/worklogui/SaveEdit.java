package com.example.worklogui;

import java.io.*;
import java.util.Map;

public class SaveEdit {

    public static void writeLogFile(File logFile, Map<String, WorkLogEntry> logEntries) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
            if (logEntries.isEmpty()) {
                writer.write("No entries found.");
                return;
            }

            for (WorkLogEntry entry : logEntries.values()) {
                writer.write(entry.getDate());
                writer.newLine();
                writer.write("SOSI: " + String.format("%.2f", entry.getSosiHours()) + " hours");
                writer.newLine();
                writer.write("Lion Bridge: " + String.format("%.2f", entry.getLionBridgeMinutes()) + " minutes");
                writer.newLine();
                writer.newLine();
            }
        }
    }
}
