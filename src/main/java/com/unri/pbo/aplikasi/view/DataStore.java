/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


package com.unri.pbo.aplikasi.view;

import com.unri.pbo.Produk;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
/**
 *
 * @author M S I
 */

public class DataStore {
    // Menyimpan daftar objek Produk asli dari library dosen
    public static ArrayList<Produk> listProduk = new ArrayList<>();
    // Menyimpan kategori berdasarkan kode produk
    public static HashMap<String, String> mapKategori = new HashMap<>();

    // Data awal contoh (otomatis terisi saat pertama kali dijalankan)
    static {
        try {
            tambahProduk(new Produk("BRG001", "Laptop", new BigDecimal("4000000"), new BigDecimal("5000000"), 10), "Elektronik");
            tambahProduk(new Produk("BRG002", "Mouse", new BigDecimal("50000"), new BigDecimal("100000"), 20), "Aksesoris");
        } catch (Exception e) {
            System.out.println("Error init data: " + e.getMessage());
        }
    }

    public static void tambahProduk(Produk p, String kategori) {
        String sql = "INSERT INTO produk (kode_produk, nama_produk, kategori, harga_beli, harga_jual, stok) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getIdProduk());
            ps.setString(2, p.getNamaProduk());
            ps.setString(3, kategori);
            ps.setBigDecimal(4, p.getHargaBeli());
            ps.setBigDecimal(5, p.getHargaJual());
            ps.setInt(6, p.getStok());
            ps.executeUpdate();

            mapKategori.put(p.getIdProduk(), kategori);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menyimpan produk ke database:\n" + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }
     public static void editProduk(Produk pBaru, String kategori) {
        String sql = "UPDATE produk SET nama_produk=?, kategori=?, harga_beli=?, harga_jual=?, stok=? WHERE kode_produk=?";
        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pBaru.getNamaProduk());
            ps.setString(2, kategori);
            ps.setBigDecimal(3, pBaru.getHargaBeli());
            ps.setBigDecimal(4, pBaru.getHargaJual());
            ps.setInt(5, pBaru.getStok());
            ps.setString(6, pBaru.getIdProduk());
            ps.executeUpdate();

            mapKategori.put(pBaru.getIdProduk(), kategori);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengubah produk di database:\n" + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void hapusProduk(String kodeProduk) {
        String sql = "DELETE FROM produk WHERE kode_produk=?";
        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, kodeProduk);
            ps.executeUpdate();
            mapKategori.remove(kodeProduk);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menghapus produk dari database:\n" + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static ArrayList<Produk> getProduk(String keyword) {
        ArrayList<Produk> hasil = new ArrayList<>();
        mapKategori.clear();

        String sql = "SELECT * FROM produk WHERE kode_produk LIKE ? OR nama_produk LIKE ?";
        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String key = "%" + keyword + "%";
            ps.setString(1, key);
            ps.setString(2, key);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String kode = rs.getString("kode_produk");
                    String nama = rs.getString("nama_produk");
                    String kategori = rs.getString("kategori");
                    BigDecimal hargaBeli = rs.getBigDecimal("harga_beli");
                    BigDecimal hargaJual = rs.getBigDecimal("harga_jual");
                    int stok = rs.getInt("stok");

                    Produk p = new Produk(kode, nama, hargaBeli, hargaJual, stok);
                    hasil.add(p);
                    mapKategori.put(kode, kategori != null ? kategori : "-");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil data produk dari database:\n" + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }
        return hasil;
    
    }
}