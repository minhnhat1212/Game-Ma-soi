package com.werewolf.server.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Quản lý kết nối database MySQL
 */
public class DatabaseConfig {
    private static final String CONFIG_FILE = "/database.properties";
    private static String url;
    private static String username;
    private static String password;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Không tìm thấy file database.properties");
            }
            Properties props = new Properties();
            props.load(input);
            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc cấu hình database", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
