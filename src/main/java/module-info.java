module ru.itis {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires java.datatransfer;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;


    opens ru.itis.client to javafx.fxml;
    exports ru.itis.gui.controllers;
    opens ru.itis.gui.controllers to javafx.fxml;
    exports ru.itis.gui;
    opens ru.itis.gui to javafx.fxml;
    exports ru.itis.core;
    opens ru.itis.protocol.holders to com.fasterxml.jackson.databind;
    exports ru.itis.gui.components;
}