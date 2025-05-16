
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


    public BillsEditorUI(String yearMonthKey, List<Bill> filteredBills, CompanyManagerService service,
                         Stage parentStage, Runnable onClose) {
        this.yearMonth = yearMonthKey;
        this.service = service;
        this.parentStage = parentStage;
        this.onClose = onClose;
        this.onSaveCallback = onClose;

        System.out.println("🔍 DEBUG: BillsEditorUI created with " + filteredBills.size() +
                " filtered bills for context: " + yearMonthKey);

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
            String editedYear = null;
            String editedMonth = null;

            if (!bills.isEmpty()) {
                // Sort by date to get the most recently edited bill
                List<Bill> sortedBills = new ArrayList<>(bills);
                sortedBills.sort(Comparator.comparing(Bill::getDate).reversed());
                Bill lastEditedBill = sortedBills.get(0);

                editedYear = String.valueOf(lastEditedBill.getDate().getYear());
                editedMonth = String.format("%02d", lastEditedBill.getDate().getMonthValue());
            }
// After editing a bill, log what values are being passed back
            System.out.println("DEBUG: Bill edited - passing year: " + editedYear + ", month: " + editedMonth);
            onSaveWithFilter.accept(editedYear, editedMonth);
            // Call the callback with the year and month to filter
        };

        // Add all the filtered bills to our observable list
        bills.addAll(filteredBills);
    }

    public void show() {
        setupTableColumns();
        billsTable.setItems(bills);

        Button addBtn = new Button("Add Bill");
        Button editBtn = new Button("Edit Bill");
        Button removeBtn = new Button("Remove Bill");
        Button saveBtn = new Button("Save Changes");
        Button closeBtn = new Button("Cancel");

        addBtn.setOnAction(e -> openBillEditor(null));
        editBtn.setOnAction(e -> {
            Bill selected = billsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openBillEditor(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to edit.");
            }
        });

        removeBtn.setOnAction(e -> {
            Bill selected = billsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                System.out.println("🔍 DEBUG: Attempting to remove bill: " + selected.getDescription());
                System.out.println("🔍 DEBUG: Current bills count: " + bills.size());

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Remove Bill");
                confirm.setHeaderText("Are you sure you want to remove this bill?");
                confirm.setContentText("This action cannot be undone.");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Get the month of the bill before removing it
                    String billMonth = String.format("%d-%02d",
                            selected.getDate().getYear(),
                            selected.getDate().getMonthValue());

                    boolean removed = bills.remove(selected);

                    if (removed) {
                        // Track that this month had bills removed
                        monthsWithRemovedBills.add(billMonth);

                        // Check if this was the last bill for this month
                        boolean monthHasRemainingBills = false;
                        for (Bill b : bills) {
                            String month = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
                            if (month.equals(billMonth)) {
                                monthHasRemainingBills = true;
                                break;
                            }
                        }

                        if (!monthHasRemainingBills) {
                            System.out.println("All bills removed from month: " + billMonth);
                        }
                    }

                    ObservableList<Bill> freshList = FXCollections.observableArrayList(new ArrayList<>(bills));
                    billsTable.setItems(freshList);
                    billsTable.refresh();
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to remove.");
            }
        });

        saveBtn.setOnAction(e -> {
            try {
                service.clearBillCache();

                // Build a map of all months that have bills
                Map<String, List<Bill>> currentMonths = new HashMap<>();
                for (Bill b : bills) {
                    String ym = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
                    currentMonths.computeIfAbsent(ym, k -> new ArrayList<>()).add(b);
                }

                // Process bills by month
                if (bills.isEmpty() && yearMonth != null && !yearMonth.equals("Multiple")) {
                    service.setBillsForMonth(yearMonth, new ArrayList<>());
                } else if (yearMonth.equals("Multiple")) {
                    List<Bill> billsCopy = new ArrayList<>(bills);
                    billsCopy.sort(Comparator.comparing(Bill::getDate));

                    Map<String, List<Bill>> grouped = new HashMap<>();
                    for (Bill b : billsCopy) {
                        String ym = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
                        grouped.computeIfAbsent(ym, k -> new ArrayList<>()).add(b);
                    }

                    for (Map.Entry<String, List<Bill>> entry : grouped.entrySet()) {
                        service.setBillsForMonth(entry.getKey(), entry.getValue());
                    }

                    for (String month : monthsWithRemovedBills) {
                        if (!currentMonths.containsKey(month)) {
                            service.setBillsForMonth(month, new ArrayList<>());
                        }
                    }
                } else {
                    // We're in a specific month mode with bills - normal processing
                    List<Bill> billsCopy = new ArrayList<>(bills);
                    billsCopy.sort(Comparator.comparing(Bill::getDate));

                    Map<String, List<Bill>> grouped = new HashMap<>();
                    for (Bill b : billsCopy) {
                        String ym = String.format("%d-%02d", b.getDate().getYear(), b.getDate().getMonthValue());
                        grouped.computeIfAbsent(ym, k -> new ArrayList<>()).add(b);
                    }

                    for (Map.Entry<String, List<Bill>> entry : grouped.entrySet()) {
                        service.setBillsForMonth(entry.getKey(), entry.getValue());
                    }

                    // Ensure current month is saved even if grouping didn't include it
                    if (!grouped.containsKey(yearMonth)) {
                        service.setBillsForMonth(yearMonth, new ArrayList<>());
                    }
                }

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                ((Stage) saveBtn.getScene().getWindow()).close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save bills: " + ex.getMessage());
            }
        });

        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());

        HBox buttons = new HBox(10, addBtn, editBtn, removeBtn, saveBtn, closeBtn);
        buttons.setPadding(new Insets(10));

        VBox layout = new VBox(10,
                new Label(yearMonth != null && yearMonth.equals("Multiple") ? "Viewing all bills (merged)" : "Editing bills for " + yearMonth),
                billsTable,
                buttons);
        layout.setPadding(new Insets(15));

        Stage stage = new Stage();
        stage.setTitle(AppConstants.APP_TITLE + " - Edit Bills");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setScene(new Scene(layout, 700, 400));
        if (getClass().getResource("/style.css") != null) {
            stage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
        stage.show();
    }

    private void openBillEditor(Bill existingBill) {
        System.out.println("Opening bill editor for " +
                (existingBill != null ? "existing bill: " + existingBill.getDescription() : "new bill"));

        Dialog<Bill> dialog = new Dialog<>();
        dialog.setTitle(existingBill == null ? "Add Bill" : "Edit Bill");

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

        TextField descField = new TextField();
        TextField amountField = new TextField();
        CheckBox paidCheck = new CheckBox("Paid");

        // Category dropdown
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
                    if (item.isDeductible()) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: red;");
                    }
                }
            }
        });
        categoryCombo.setButtonCell(categoryCombo.getCellFactory().call(null));

        if (existingBill != null) {
            datePicker.setValue(existingBill.getDate());
            descField.setText(existingBill.getDescription());
            amountField.setText(String.format("%.2f", existingBill.getAmount()));
            paidCheck.setSelected(existingBill.isPaid());
            categoryCombo.setValue(existingBill.getCategory());
        } else {
            datePicker.setValue(LocalDate.now());
            categoryCombo.setValue(ExpenseCategory.NONE);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Amount:"), 0, 2);
        grid.add(amountField, 1, 2);
        grid.add(paidCheck, 1, 3);
        grid.add(new Label("Category:"), 0, 4);
        grid.add(categoryCombo, 1, 4);

        // Category help text
        Label helpText = new Label("Green = Deductible, Red = Non-deductible");
        helpText.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
        grid.add(helpText, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    String description = descField.getText().trim();
                    double amount = Double.parseDouble(amountField.getText().trim());
                    LocalDate date = datePicker.getValue();
                    boolean paid = paidCheck.isSelected();
                    ExpenseCategory category = categoryCombo.getValue();

                    if (date == null) throw new IllegalArgumentException("Date is required");
                    if (description.isEmpty()) throw new IllegalArgumentException("Description is required");
                    if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
                    if (category == null) throw new IllegalArgumentException("Category is required");

                    if (existingBill != null) {
                        existingBill.setDate(date);
                        existingBill.setDescription(description);
                        existingBill.setAmount(amount);
                        existingBill.setPaid(paid);
                        existingBill.setCategory(category);
                        return existingBill;
                    } else {
                        return new Bill(date, description, amount, paid, category);
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", e.getMessage());
                }
            }
            return null;
        });

        Optional<Bill> result = dialog.showAndWait();
        result.ifPresent(bill -> {
            if (existingBill == null) {
                bills.add(bill);
            } else {
                System.out.println("Existing bill updated in list");
            }
            bills.sort(Comparator.comparing(Bill::getDate));

            // Create a fresh list to force UI update
            ObservableList<Bill> freshList = FXCollections.observableArrayList(new ArrayList<>(bills));
            billsTable.setItems(freshList);
            billsTable.refresh();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void setupTableColumns() {
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

        TableColumn<Bill, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescription()));
        descCol.setPrefWidth(200);

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

        // Update category column
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
                    // Color code based on deductibility
                    if (category.isDeductible()) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: red;");
                    }
                }
            }
        });
        categoryCol.setPrefWidth(150);

        billsTable.getColumns().addAll(dateCol, descCol, amountCol, paidCol, categoryCol);
    }

}