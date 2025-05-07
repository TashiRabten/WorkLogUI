package com.example.worklogui;

public class LineUtils {

    public static boolean isValidDate(String date) {
        return date.matches("^(0[1-9]|1[0-2])/([0-2][0-9]|3[01])/\\d{4}$");
    }

    public static double parseSosi(String line) {
        return Double.parseDouble(line.replace("SOSI:", "").trim().replace("hours", "").trim());
    }

    public static double parseLionBridge(String line) {
        return Double.parseDouble(line.replace("Lion Bridge:", "").trim().replace("minutes", "").trim());
    }
}
