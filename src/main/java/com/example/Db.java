package com.example;

import java.sql.*;

public class Db {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ArxivDatabase";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS Pages (" +
                    "id SERIAL PRIMARY KEY," +
                    "title TEXT NOT NULL," +
                    "authors TEXT," +
                    "content TEXT," +
                    "url TEXT UNIQUE)";
            stmt.execute(sql);

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    public static void savePaper(String title, String authors, String content, String url) {
        String sql = "INSERT INTO Pages(title, authors, content, url) VALUES(?,?,?,?) " +
                "ON CONFLICT (url) DO UPDATE SET " +
                "title = EXCLUDED.title, authors = EXCLUDED.authors, content = EXCLUDED.content";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, authors);
            pstmt.setString(3, content);
            pstmt.setString(4, url);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error saving paper: " + e.getMessage());
        }
    }
}
