package com.unri.pbo.aplikasi.view;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Koneksi {
    private static final String URL = "jdbc:mysql://localhost:3306/pos_toko_berkat?serverTimezone=Asia/Jakarta";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void main(String[] args) {
        try (Connection c = getConnection()) {
            System.out.println("Koneksi berhasil");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}