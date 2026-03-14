package de.kluecki.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:sqlserver://localhost\\RONALD;" +
                    "databaseName=QuellenDB;" +
                    "encrypt=true;" +
                    "trustServerCertificate=true;" +
                    "user=java_test;" +
                    "password=JavaTest123!";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}