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
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.sql;

    opens com.example.worklogui to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.example.worklogui;
}