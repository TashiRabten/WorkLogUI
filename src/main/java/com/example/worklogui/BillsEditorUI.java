
package com.example.worklogui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;

public class BillsEditorUI {

    private final String yearMonth;
    private final CompanyManagerService service;
    private final Stage parentStage;
    private final Runnable onClose;
    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final TableView<Bill> billsTable = new TableView<>();

    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final Runnable onSaveCallback;
    private final Set<String> monthsWithRemovedBills = new HashSet<>();
    private Bill mostRecentlyAddedBill = null;
    private boolean billsEdited = false;

    public BillsEditorUI(String yearMonthKey, List<Bill> filteredBills, CompanyManagerService service,
                         Stage parentStage, Runnable onClose) {
        this.yearMonth = yearMonthKey;
        this.service = service;
        this.parentStage = parentStage;
        this.onClose = onClose;
        this.onSaveCallback = onClose;


        // Add all the filtered bills to our observable list
        bills.addAll(filteredBills);
    }

    // New constructor accepts a BiConsumer<String, String> for year and month to filter
    public BillsEditorUI(String yearMonthKey, List<Bill> filteredBills, CompanyManagerService service,
                         Stage parentStage, BiConsumer<String, String> onSaveWithFilter) {
        this.yearMonth = yearMonthKey;
        this.service = service;
        this.parentStage = parentStage;
        this.onClose = () -> {};

        this.onSaveCallback = () -> {
            // Only update filters if additions or edits occurred (not just removals)
            if (billsEdited || mostRecentlyAddedBill != null) {
                String editedYear = null;
                String editedMonth = null;

                // If we have a recently added bill, use its date
                if (mostRecentlyAddedBill != null) {
                    editedYear = String.valueOf(mostRecentlyAddedBill.getDate().getYear());
                    editedMonth = String.format("%02d", mostRecentlyAddedBill.getDate().getMonthValue());
                }
                // Otherwise, fall back to the most recent bill by date if edits occurred
                else if (billsEdited && !bills.isEmpty()) {
                    List<Bill> sortedBills = new ArrayList<>(bills);
                    sortedBills.sort(Comparator.comparing(Bill::getDate).reversed());
                    Bill lastEditedBill = sortedBills.get(0);

                    editedYear = String.valueOf(lastEditedBill.getDate().getYear());
                    editedMonth = String.format("%02d", lastEditedBill.getDate().getMonthValue());
                }

                if (editedYear != null && editedMonth != null) {
                    onSaveWithFilter.accept(editedYear, editedMonth);
                } else {
                    // No filter update needed, just call without filters
                    onSaveWithFilter.accept(null, null);
                }
            } else {
                // No additions or edits occurred, just call without filters
                onSaveWithFilter.accept(null, null);
            }

            // Reset tracking variables
            mostRecentlyAddedBill = null;
            billsEdited = false;
        };

        // Add all the filtered bills to our observable list
        bills.addAll(filteredBills);
    }

    public void show() {
        setupTableColumns();
        billsTable.setItems(bills);

        HBox buttons = createButtonPanel();
        VBox layout = createMainLayout(buttons);
        
        Stage stage = createAndConfigureStage(layout);
        stage.show();
    }

    private HBox createButtonPanel() {
        Button addBtn = new Button("Add Bill");
        Button editBtn = new Button("Edit Bill");
        Button removeBtn = new Button("Remove Bill");
        Button saveBtn = new Button("Save Changes");
        Button closeBtn = new Button("Cancel");

        setupButtonActions(addBtn, editBtn, removeBtn, saveBtn, closeBtn);

        HBox buttons = new HBox(10, addBtn, editBtn, removeBtn, saveBtn, closeBtn);
        buttons.setPadding(new Insets(10));
        return buttons;
    }

    private VBox createMainLayout(HBox buttons) {
        VBox layout = new VBox(10,
                new Label(yearMonth != null && yearMonth.equals("Multiple") ? "Viewing all bills (merged)" : "Editing bills for " + yearMonth),
                billsTable,
                buttons);
        layout.setPadding(new Insets(15));
        return layout;
    }

    private Stage createAndConfigureStage(VBox layout) {
        Stage stage = new Stage();
        stage.setTitle(AppConstants.APP_TITLE + " - Edit Bills");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setScene(new Scene(layout, 700, 400));
        if (getClass().getResource("/style.css") != null) {
            stage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
        
        // Make resizable with minimum size
        stage.setResizable(true);
        stage.setMinWidth(700);
        stage.setMinHeight(400);
        
        return stage;
    }

    private void setupButtonActions(Button addBtn, Button editBtn, Button removeBtn, Button saveBtn, Button closeBtn) {
        addBtn.setOnAction(e -> openBillEditor(null));
        editBtn.setOnAction(e -> handleEditButton());
        removeBtn.setOnAction(e -> handleRemoveButton());
        saveBtn.setOnAction(e -> handleSaveButton(saveBtn));
        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());
    }

    private void handleEditButton() {
        Bill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openBillEditor(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to edit.");
        }
    }

    private void handleRemoveButton() {
        Bill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            performBillRemoval(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to remove.");
        }
    }

    private void performBillRemoval(Bill selected) {

        if (confirmBillRemoval()) {
            String billMonth = String.format("%d-%02d",
                    selected.getDate().getYear(),
                    selected.getDate().getMonthValue());

            boolean removed = bills.remove(selected);

            if (removed) {
                trackRemovedBillMonth(billMonth);
                refreshBillsTable();
            }
        }
    }

    private boolean confirmBillRemoval() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Bill");
        confirm.setHeaderText("Are you sure you want to remove this bill?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void trackRemovedBillMonth(String billMonth) {
        monthsWithRemovedBills.add(billMonth);

        boolean monthHasRemainingBills = bills.stream()
                .anyMatch(b -> {
                    String month = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
                    return month.equals(billMonth);
                });

        if (!monthHasRemainingBills) {
            System.out.println("All bills removed from month: " + billMonth);
        }
    }

    private void refreshBillsTable() {
        bills.sort(Comparator.comparing(Bill::getDate));
        ObservableList<Bill> freshList = FXCollections.observableArrayList(new ArrayList<>(bills));
        billsTable.setItems(freshList);
        billsTable.refresh();
    }

    private void handleSaveButton(Button saveBtn) {
        try {
            service.clearBillCache();
            saveBillsData();

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            ((Stage) saveBtn.getScene().getWindow()).close();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save bills: " + ex.getMessage());
        }
    }

    private void saveBillsData() throws Exception {
        Map<String, List<Bill>> currentMonths = buildCurrentMonthsMap();

        if (bills.isEmpty() && yearMonth != null && !yearMonth.equals("Multiple")) {
            service.setBillsForMonth(yearMonth, new ArrayList<>());
        } else if (yearMonth.equals("Multiple")) {
            saveMultipleMonthsBills(currentMonths);
        } else {
            saveSpecificMonthBills();
        }
    }

    private Map<String, List<Bill>> buildCurrentMonthsMap() {
        Map<String, List<Bill>> currentMonths = new HashMap<>();
        for (Bill b : bills) {
            String ym = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
            currentMonths.computeIfAbsent(ym, k -> new ArrayList<>()).add(b);
        }
        return currentMonths;
    }

    private void saveMultipleMonthsBills(Map<String, List<Bill>> currentMonths) throws Exception {
        Map<String, List<Bill>> grouped = groupBillsByMonth();

        for (Map.Entry<String, List<Bill>> entry : grouped.entrySet()) {
            service.setBillsForMonth(entry.getKey(), entry.getValue());
        }

        for (String month : monthsWithRemovedBills) {
            if (!currentMonths.containsKey(month)) {
                service.setBillsForMonth(month, new ArrayList<>());
            }
        }
    }

    private void saveSpecificMonthBills() throws Exception {
        Map<String, List<Bill>> grouped = groupBillsByMonth();

        for (Map.Entry<String, List<Bill>> entry : grouped.entrySet()) {
            service.setBillsForMonth(entry.getKey(), entry.getValue());
        }

        if (!grouped.containsKey(yearMonth)) {
            service.setBillsForMonth(yearMonth, new ArrayList<>());
        }
    }

    private Map<String, List<Bill>> groupBillsByMonth() {
        List<Bill> billsCopy = new ArrayList<>(bills);
        billsCopy.sort(Comparator.comparing(Bill::getDate));

        Map<String, List<Bill>> grouped = new HashMap<>();
        for (Bill b : billsCopy) {
            String ym = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
            grouped.computeIfAbsent(ym, k -> new ArrayList<>()).add(b);
        }
        return grouped;
    }
    private void openBillEditor(Bill existingBill) {
        System.out.println("Opening bill editor for " +
                (existingBill != null ? "existing bill: " + existingBill.getDescription() : "new bill"));

        Dialog<Bill> dialog = createBillDialog(existingBill);
        BillEditorFields fields = createBillEditorFields();
        populateFields(fields, existingBill);
        setupDialogLayout(dialog, fields);
        configureBillDialogResultConverter(dialog, fields, existingBill);
        
        Optional<Bill> result = dialog.showAndWait();
        handleBillDialogResult(result, existingBill);
    }
    
    private Dialog<Bill> createBillDialog(Bill existingBill) {
        Dialog<Bill> dialog = new Dialog<>();
        dialog.setTitle(existingBill == null ? "Add Bill" : "Edit Bill");
        return dialog;
    }
    
    private BillEditorFields createBillEditorFields() {
        DatePicker datePicker = createDatePicker();
        TextField descField = new TextField();
        TextField amountField = new TextField();
        CheckBox paidCheck = new CheckBox("Paid");
        ComboBox<ExpenseCategory> categoryCombo = createCategoryComboBox();
        
        return new BillEditorFields(datePicker, descField, amountField, paidCheck, categoryCombo);
    }
    
    private DatePicker createDatePicker() {
        DatePicker datePicker = new DatePicker();
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(displayFormatter) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                try {
                    return (string != null && !string.isEmpty()) ? LocalDate.parse(string, displayFormatter) : null;
                } catch (Exception e) {
                    return null;
                }
            }
        });
        return datePicker;
    }
    
    private ComboBox<ExpenseCategory> createCategoryComboBox() {
        ComboBox<ExpenseCategory> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(ExpenseCategory.values());
        categoryCombo.setCellFactory(lv -> new ListCell<ExpenseCategory>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getDisplayName());
                    setStyle(item.isDeductible() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });
        categoryCombo.setButtonCell(categoryCombo.getCellFactory().call(null));
        return categoryCombo;
    }
    
    private void populateFields(BillEditorFields fields, Bill existingBill) {
        if (existingBill != null) {
            fields.datePicker.setValue(existingBill.getDate());
            fields.descField.setText(existingBill.getDescription());
            fields.amountField.setText(String.format("%.2f", existingBill.getAmount()));
            fields.paidCheck.setSelected(existingBill.isPaid());
            fields.categoryCombo.setValue(existingBill.getCategory());
        } else {
            fields.datePicker.setValue(LocalDate.now());
            fields.categoryCombo.setValue(ExpenseCategory.NONE);
        }
    }
    
    private void setupDialogLayout(Dialog<Bill> dialog, BillEditorFields fields) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Date:"), 0, 0);
        grid.add(fields.datePicker, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(fields.descField, 1, 1);
        grid.add(new Label("Amount:"), 0, 2);
        grid.add(fields.amountField, 1, 2);
        grid.add(fields.paidCheck, 1, 3);
        grid.add(new Label("Category:"), 0, 4);
        grid.add(fields.categoryCombo, 1, 4);

        Label helpText = new Label("Green = Deductible, Red = Non-deductible");
        helpText.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
        grid.add(helpText, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }
    
    private void configureBillDialogResultConverter(Dialog<Bill> dialog, BillEditorFields fields, Bill existingBill) {
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    return createOrUpdateBill(fields, existingBill);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", e.getMessage());
                }
            }
            return null;
        });
    }
    
    private Bill createOrUpdateBill(BillEditorFields fields, Bill existingBill) {
        String description = fields.descField.getText().trim();
        double amount = Double.parseDouble(fields.amountField.getText().trim());
        LocalDate date = fields.datePicker.getValue();
        boolean paid = fields.paidCheck.isSelected();
        ExpenseCategory category = fields.categoryCombo.getValue();

        validateBillInput(date, description, amount, category);

        if (existingBill != null) {
            updateExistingBill(existingBill, date, description, amount, paid, category);
            return existingBill;
        } else {
            return new Bill(date, description, amount, paid, category);
        }
    }
    
    private void validateBillInput(LocalDate date, String description, double amount, ExpenseCategory category) {
        if (date == null) throw new IllegalArgumentException("Date is required");
        if (description.isEmpty()) throw new IllegalArgumentException("Description is required");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (category == null) throw new IllegalArgumentException("Category is required");
    }
    
    private void updateExistingBill(Bill bill, LocalDate date, String description, double amount, boolean paid, ExpenseCategory category) {
        bill.setDate(date);
        bill.setDescription(description);
        bill.setAmount(amount);
        bill.setPaid(paid);
        bill.setCategory(category);
    }
    
    private void handleBillDialogResult(Optional<Bill> result, Bill existingBill) {
        result.ifPresent(bill -> {
            if (existingBill == null) {
                addNewBill(bill);
            } else {
                System.out.println("Existing bill updated in list");
                billsEdited = true;
            }
            refreshBillsTable();
        });
    }
    
    private void addNewBill(Bill bill) {
        bills.add(bill);
        mostRecentlyAddedBill = bill;
        billsEdited = true;
    }
    
    private static class BillEditorFields {
        final DatePicker datePicker;
        final TextField descField;
        final TextField amountField;
        final CheckBox paidCheck;
        final ComboBox<ExpenseCategory> categoryCombo;
        
        BillEditorFields(DatePicker datePicker, TextField descField, TextField amountField, 
                        CheckBox paidCheck, ComboBox<ExpenseCategory> categoryCombo) {
            this.datePicker = datePicker;
            this.descField = descField;
            this.amountField = amountField;
            this.paidCheck = paidCheck;
            this.categoryCombo = categoryCombo;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void setupTableColumns() {
        billsTable.getColumns().clear();
        
        TableColumn<Bill, LocalDate> dateCol = createDateColumn();
        TableColumn<Bill, String> descCol = createDescriptionColumn();
        TableColumn<Bill, Double> amountCol = createAmountColumn();
        TableColumn<Bill, Boolean> paidCol = createPaidColumn();
        TableColumn<Bill, ExpenseCategory> categoryCol = createCategoryColumn();

        billsTable.getColumns().addAll(dateCol, descCol, amountCol, paidCol, categoryCol);
        billsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private TableColumn<Bill, LocalDate> createDateColumn() {
        TableColumn<Bill, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getDate()));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText((empty || date == null) ? null : date.format(displayFormatter));
            }
        });
        dateCol.setPrefWidth(100);
        return dateCol;
    }

    private TableColumn<Bill, String> createDescriptionColumn() {
        TableColumn<Bill, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescription()));
        descCol.setPrefWidth(200);
        return descCol;
    }

    private TableColumn<Bill, Double> createAmountColumn() {
        TableColumn<Bill, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getAmount()));
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText((empty || amount == null) ? null : String.format("$%.2f", amount));
            }
        });
        amountCol.setPrefWidth(100);
        return amountCol;
    }

    private TableColumn<Bill, Boolean> createPaidColumn() {
        TableColumn<Bill, Boolean> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().isPaid()));
        paidCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean paid, boolean empty) {
                super.updateItem(paid, empty);
                if (empty || paid == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(paid);
                    checkBox.setDisable(true);
                    setGraphic(checkBox);
                }
            }
        });
        paidCol.setPrefWidth(50);
        return paidCol;
    }

    private TableColumn<Bill, ExpenseCategory> createCategoryColumn() {
        TableColumn<Bill, ExpenseCategory> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ExpenseCategory category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(category.getDisplayName());
                    setStyle(category.isDeductible() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });
        categoryCol.setPrefWidth(150);
        return categoryCol;
    }

}