package com.example.worklogui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BillsEditorUI {

    private final String yearMonth;
    private final CompanyManagerService service;
    private final Stage owner;

    public BillsEditorUI(String yearMonth, CompanyManagerService service, Stage owner) {
        this.yearMonth = yearMonth;
        this.service = service;
        this.owner = owner;
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.setTitle("Edit Bills for " + yearMonth);
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);

        ObservableList<Bill> billList = FXCollections.observableArrayList(service.getBillsForMonth(yearMonth));

        ListView<Bill> listView = new ListView<>(billList);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Bill bill, boolean empty) {
                super.updateItem(bill, empty);
                if (empty || bill == null) {
                    setText(null);
                } else {
                    setText(bill.getLabel() + ": $" + String.format("%.2f", bill.getAmount()) +
                            " (" + bill.getDate() + ")");
                }
            }
        });

        Button addBtn = new Button("+ Add");
        Button editBtn = new Button("✏ Edit");
        Button deleteBtn = new Button("🗑 Remove");
        Button saveBtn = new Button("✔ Save");

        addBtn.setOnAction(e -> openBillDialog(null, billList));
        editBtn.setOnAction(e -> {
            Bill selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openBillDialog(selected, billList);
            }
        });
        deleteBtn.setOnAction(e -> {
            Bill selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                billList.remove(selected);
            }
        });
        saveBtn.setOnAction(e -> {
            try {
                service.setBillsForMonth(yearMonth, billList);
                dialog.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save bills: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        HBox buttons = new HBox(10, addBtn, editBtn, deleteBtn, saveBtn);
        VBox root = new VBox(10, listView, buttons);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 420, 350);
        dialog.setScene(scene);
        dialog.show();
    }

    private void openBillDialog(Bill billToEdit, ObservableList<Bill> billList) {
        Dialog<Bill> dialog = new Dialog<>();
        dialog.setTitle(billToEdit == null ? "Add Bill" : "Edit Bill");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField labelField = new TextField();
        TextField amountField = new TextField();
        DatePicker datePicker = new DatePicker();

        if (billToEdit != null) {
            labelField.setText(billToEdit.getLabel());
            amountField.setText(String.valueOf(billToEdit.getAmount()));
            datePicker.setValue(billToEdit.getDate());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Label:"), 0, 0);
        grid.add(labelField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    double amount = Double.parseDouble(amountField.getText().trim());
                    String label = labelField.getText().trim();
                    LocalDate date = datePicker.getValue();
                    if (label.isEmpty() || date == null) return null;
                    return new Bill(label, amount, date);
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Bill> result = dialog.showAndWait();
        result.ifPresent(newBill -> {
            if (billToEdit != null) {
                billList.remove(billToEdit);
            }
            billList.add(newBill);
            billList.sort((b1, b2) -> b1.getDate().compareTo(b2.getDate()));
        });
    }
}
