module com.example.worklogui {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires java.desktop;
    requires java.base;
    requires org.json;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;


    opens com.example.worklogui to javafx.fxml;
    exports com.example.worklogui;
}