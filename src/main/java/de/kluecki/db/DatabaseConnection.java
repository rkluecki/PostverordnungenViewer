/*
 * Zentrale Datenbankverbindung.
 *
 * Liefert Connection für alle Repository Klassen.
 *
 * Wichtig:
 * Änderungen hier wirken sich auf gesamte DB Schicht aus.
 */

package de.kluecki.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:sqlserver://localhost\\SQL2025;" +
                    "databaseName=QuellenDB;" +
                    "encrypt=true;" +
                    "trustServerCertificate=true;" +
                    "user=java_test;" +
                    "password=08-Leo*-12";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

