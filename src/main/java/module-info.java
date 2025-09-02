module com.mahmud {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens com.mahmud.controller to javafx.fxml;
    opens com.mahmud.model to javafx.fxml;

    exports com.mahmud;
}
