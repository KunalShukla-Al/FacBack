package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The one place that opens a database connection. Settings come from the
 * environment, with local defaults: FACBACK_DB_URL, FACBACK_DB_USER and
 * FACBACK_DB_PASSWORD.
 */
public final class Db {

    private static final String URL = env("FACBACK_DB_URL", "jdbc:mysql://localhost:3306/facback");
    private static final String USER = env("FACBACK_DB_USER", "root");
    private static final String PASSWORD = env("FACBACK_DB_PASSWORD", "");

    private Db() {}

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
