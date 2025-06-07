package com.example.worklogui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
        var allBills = service.getAllBills();
        int migratedCount = processBillMigration(service, allBills);
        
        System.out.println("✅ Migrated " + migratedCount + " bills to category system.");
    }

    private static int processBillMigration(CompanyManagerService service, Map<String, List<Bill>> allBills) {
        int migratedCount = 0;
        
        for (var entry : allBills.entrySet()) {
            String yearMonth = entry.getKey();
            List<Bill> bills = entry.getValue();
            
            MigrationResult result = migrateBillsForMonth(bills);
            if (result.needsUpdate) {
                saveMigratedBills(service, yearMonth, bills);
                migratedCount += result.migratedCount;
            }
        }
        
        return migratedCount;
    }

    private static MigrationResult migrateBillsForMonth(List<Bill> bills) {
        boolean needsUpdate = false;
        int migratedCount = 0;
        
        for (Bill bill : bills) {
            if (requiresMigration(bill)) {
                migrateSingleBill(bill);
                needsUpdate = true;
                migratedCount++;
                System.out.println("  Migrated: " + bill.getDescription() + " -> " + bill.getCategory());
            }
        }
        
        return new MigrationResult(needsUpdate, migratedCount);
    }

    private static boolean requiresMigration(Bill bill) {
        return bill.getCategory() == null || bill.getCategory() == ExpenseCategory.NONE;
    }

    private static void migrateSingleBill(Bill bill) {
        bill.initializeCategory();
        
        if (bill.getCategory() == ExpenseCategory.NONE || bill.getCategory() == ExpenseCategory.OTHER_DEDUCTIBLE) {
            ExpenseCategory autoCategory = autoCategorizeBill(bill.getDescription());
            bill.setCategory(autoCategory);
        }
    }

    private static void saveMigratedBills(CompanyManagerService service, String yearMonth, List<Bill> bills) {
        try {
            service.setBillsForMonth(yearMonth, bills);
            System.out.println("  ✅ Updated " + yearMonth);
        } catch (Exception e) {
            System.err.println("  ❌ Failed to update " + yearMonth + ": " + e.getMessage());
        }
    }

    private static class MigrationResult {
        final boolean needsUpdate;
        final int migratedCount;
        
        MigrationResult(boolean needsUpdate, int migratedCount) {
            this.needsUpdate = needsUpdate;
            this.migratedCount = migratedCount;
        }
    }

    /**
     * Auto-categorize bills based on description keywords
     */
    public static ExpenseCategory autoCategorizeBill(String description) {
        if (description == null || description.isEmpty()) {
            return ExpenseCategory.OTHER_DEDUCTIBLE;
        }

        String desc = description.toLowerCase();

        return findMatchingCategory(desc);
    }

    private static ExpenseCategory findMatchingCategory(String description) {
        // Check each category in order of specificity
        ExpenseCategory category = checkInsuranceCategory(description);
        if (category != null) return category;

        category = checkUtilitiesCategory(description);
        if (category != null) return category;

        category = checkCommunicationCategory(description);
        if (category != null) return category;

        category = checkOfficeCategory(description);
        if (category != null) return category;

        category = checkEquipmentCategory(description);
        if (category != null) return category;

        category = checkTransportationCategory(description);
        if (category != null) return category;

        return ExpenseCategory.OTHER_DEDUCTIBLE;
    }

    private static ExpenseCategory checkInsuranceCategory(String description) {
        if (containsAny(description, "insurance", "premium")) {
            if (description.contains("health")) {
                return ExpenseCategory.HEALTH_INSURANCE;
            }
            return ExpenseCategory.BUSINESS_INSURANCE;
        }
        return null;
    }

    private static ExpenseCategory checkUtilitiesCategory(String description) {
        if (containsAny(description, "electric", "water", "utility", "power", "light bill")) {
            return ExpenseCategory.UTILITIES;
        }
        return null;
    }

    private static ExpenseCategory checkCommunicationCategory(String description) {
        if (containsAny(description, "phone", "internet", "cellular", "verizon", "at&t", "comcast", "t-mobile", "sprint")) {
            return ExpenseCategory.COMMUNICATION;
        }
        return null;
    }

    private static ExpenseCategory checkOfficeCategory(String description) {
        if (containsAny(description, "rent", "lease")) {
            return ExpenseCategory.OFFICE_RENT;
        }
        if (containsAny(description, "staples", "office depot", "supplies", "paper", "ink")) {
            return ExpenseCategory.OFFICE_SUPPLIES;
        }
        return null;
    }

    private static ExpenseCategory checkEquipmentCategory(String description) {
        if (containsAny(description, "computer", "laptop", "printer", "equipment", "software")) {
            return ExpenseCategory.EQUIPMENT;
        }
        return null;
    }

    private static ExpenseCategory checkTransportationCategory(String description) {
        if (containsAny(description, "gas", "fuel", "mileage", "car", "auto", "vehicle")) {
            return ExpenseCategory.BUSINESS_MILEAGE;
        }
        return null;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}