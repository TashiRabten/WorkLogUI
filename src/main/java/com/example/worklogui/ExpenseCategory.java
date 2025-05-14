package com.example.worklogui;

public enum ExpenseCategory {
    NONE("None", false),
    OFFICE_RENT("Office Rent", true),
    UTILITIES("Utilities", true),
    OFFICE_SUPPLIES("Office Supplies", true),
    EQUIPMENT("Equipment/Depreciation", true),
    BUSINESS_MILEAGE("Business Mileage", true),
    BUSINESS_TRAVEL("Business Travel", true),
    BUSINESS_INSURANCE("Business Insurance", true),
    PROFESSIONAL_SERVICES("Professional Services", true),
    ADVERTISING("Advertising/Marketing", true),
    COMMUNICATION("Phone/Internet", true),
    REPAIRS_MAINTENANCE("Repairs & Maintenance", true),
    CONTRACTOR_PAYMENTS("Contractor Payments", true),
    EDUCATION("Professional Education", true),
    BANK_FEES("Bank/Merchant Fees", true),
    BUSINESS_SUPPLIES("Business Supplies", true),
    HEALTH_INSURANCE("Health Insurance Premium", true),
    HOME_OFFICE("Home Office", true),
    OTHER_DEDUCTIBLE("Other Deductible", true),
    PERSONAL("Personal (Non-deductible)", false),
    ENTERTAINMENT("Entertainment (Limited)", true);

    private final String displayName;
    private final boolean deductible;

    ExpenseCategory(String displayName, boolean deductible) {
        this.displayName = displayName;
        this.deductible = deductible;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDeductible() {
        return deductible;
    }

    @Override
    public String toString() {
        return displayName;
    }
}