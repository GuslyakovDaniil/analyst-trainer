package com.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://db:5432/trainer_db");
            config.setUsername("admin");
            config.setPassword("secret");

            // Настройки, чтобы Hikari не падал мгновенно
            config.setInitializationFailTimeout(0);
            config.setMinimumIdle(1);
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000); // 30 секунд на ожидание

            dataSource = new HikariDataSource(config);
        }
        return dataSource.getConnection();
    }
}