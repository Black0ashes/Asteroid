module se.asteroid {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.apache.logging.log4j;
    requires java.logging;

    opens se.asteroid.controller to javafx.fxml;
    opens se.asteroid.model to javafx.fxml;

    exports se.asteroid.controller;
    exports se.asteroid.model;
}