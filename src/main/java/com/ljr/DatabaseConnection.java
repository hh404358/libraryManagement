package com.ljr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/Library?serverTimezone=GMT%2B8&useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "liujiarong";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
