package com.example.worklogui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DataMigrationUtility {

    /**
     * Runs all necessary data migrations at application startup
     */
    public static void runMigrations(CompanyManagerService service) {
        System.out.println("🔄 Running data migrations...");

        // Check if migration is needed
        if (needsMigration()) {
            migrateBillsToCategories(service);
        }

        System.out.println("✅ Data migrations complete.");
    }

    /**
     * Checks if migration is needed by looking for bills with deductible field
     */
    private static boolean needsMigration() {
        Path billsDir = AppConstants.EXPORT_FOLDER.getParent().resolve("bills");
        if (!Files.exists(billsDir)) {
            return false;
        }

        try {
            return Files.list(billsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .anyMatch(path -> {
                        try {
                            String content = Files.readString(path);
                            return content.contains("\"deductible\"");
                        } catch (IOException e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Migrates bills from old deductible boolean to new category system
     */
    public static void migrateBillsToCategories(CompanyManagerService service) {
        System.out.println("🔄 Migrating bills to category system...");

        service.clearBillCache();

        // Get all bills from all months
        var allBills = service.getAllBills();
        int migratedCount = 0;

        for (var entry : allBills.entrySet()) {
            String yearMonth = entry.getKey();
            List<Bill> bills = entry.getValue();
            boolean needsUpdate = false;

            for (Bill bill : bills) {
                // This will already be initialized by FileLoader, but let's be sure
                if (bill.getCategory() == null || bill.getCategory() == ExpenseCategory.NONE) {
                    bill.initializeCategory();

                    // If still NONE after initialization, try to auto-categorize
                    if (bill.getCategory() == ExpenseCategory.NONE || bill.getCategory() == ExpenseCategory.OTHER_DEDUCTIBLE) {
                        ExpenseCategory autoCategory = autoCategorizeBill(bill.getDescription());
                        bill.setCategory(autoCategory);
                    }

                    needsUpdate = true;
                    migratedCount++;
                    System.out.println("  Migrated: " + bill.getDescription() + " -> " + bill.getCategory());
                }
            }

            // Save updated bills
            if (needsUpdate) {
                try {
                    service.setBillsForMonth(yearMonth, bills);
                    System.out.println("  ✅ Updated " + yearMonth);
                } catch (Exception e) {
                    System.err.println("  ❌ Failed to update " + yearMonth + ": " + e.getMessage());
                }
            }
        }

        System.out.println("✅ Migrated " + migratedCount + " bills to category system.");
    }

    /**
     * Auto-categorize bills based on description keywords
     */
    public static ExpenseCategory autoCategorizeBill(String description) {
        if (description == null || description.isEmpty()) {
            return ExpenseCategory.OTHER_DEDUCTIBLE;
        }

        String desc = description.toLowerCase();

        // Utilities
        if (desc.contains("electric") || desc.contains("gas") || desc.contains("water") ||
                desc.contains("utility") || desc.contains("power") || desc.contains("light bill")) {
            return ExpenseCategory.UTILITIES;
        }

        // Phone/Internet
        if (desc.contains("phone") || desc.contains("internet") || desc.contains("cellular") ||
                desc.contains("verizon") || desc.contains("at&t") || desc.contains("comcast") ||
                desc.contains("t-mobile") || desc.contains("sprint")) {
            return ExpenseCategory.COMMUNICATION;
        }

        // Insurance
        if (desc.contains("insurance") || desc.contains("premium")) {
            if (desc.contains("health")) {
                return ExpenseCategory.HEALTH_INSURANCE;
            }
            return ExpenseCategory.BUSINESS_INSURANCE;
        }

        // Rent
        if (desc.contains("rent") || desc.contains("lease")) {
            return ExpenseCategory.OFFICE_RENT;
        }

        // Office Supplies
        if (desc.contains("staples") || desc.contains("office depot") || desc.contains("supplies") ||
                desc.contains("paper") || desc.contains("ink")) {
            return ExpenseCategory.OFFICE_SUPPLIES;
        }

        // Equipment
        if (desc.contains("computer") || desc.contains("laptop") || desc.contains("printer") ||
                desc.contains("equipment") || desc.contains("software")) {
            return ExpenseCategory.EQUIPMENT;
        }

        // Car/Mileage
        if (desc.contains("gas") || desc.contains("fuel") || desc.contains("mileage") ||
                desc.contains("car") || desc.contains("auto") || desc.contains("vehicle")) {
            return ExpenseCategory.BUSINESS_MILEAGE;
        }

        // Default to OTHER_DEDUCTIBLE for anything we can't categorize
        return ExpenseCategory.OTHER_DEDUCTIBLE;
    }
}