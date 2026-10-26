package com.bank;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections. Credentials are NOT hard-coded: they come from
 * environment variables (DB_URL, DB_USER, DB_PASSWORD) or from a local
 * db.properties file that is excluded from git.
 */
public final class DBConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/banking_db";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException ignored) {
            // File is optional; fall back to environment variables.
        }

        String url = firstNonNull(System.getenv("DB_URL"), props.getProperty("db.url"), DEFAULT_URL);
        String user = firstNonNull(System.getenv("DB_USER"), props.getProperty("db.user"), "root");
        String password = firstNonNull(System.getenv("DB_PASSWORD"), props.getProperty("db.password"), "");

        return DriverManager.getConnection(url, user, password);
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
