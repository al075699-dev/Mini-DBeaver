module ConexionConFx {

    requires javafx.controls;
    requires javafx.fxml;
    requires mysql.connector.j;
    requires com.jcraft.jsch;
    requires org.mariadb.jdbc;
    requires java.desktop;

    opens main to javafx.fxml, javafx.controls, javafx.graphics;
    opens controllers to javafx.fxml;
}

