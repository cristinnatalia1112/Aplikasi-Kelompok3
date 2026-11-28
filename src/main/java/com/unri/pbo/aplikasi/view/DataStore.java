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
import java.util.List;
import java.util.ArrayList;
import com.unri.pbo.Penjualan;
import com.unri.pbo.Pelanggan;
/**
 *
 * @author M S I
 */

public class DataStore {
    // Menyimpan daftar objek Produk asli dari library dosen
    public static ArrayList<Produk> listProduk = new ArrayList<>();
    // Menyimpan kategori berdasarkan kode produk
    public static HashMap<String, String> mapKategori = new HashMap<>();

    
    

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
        // ================= METHOD UNTUK DASHBOARD =================

    public static int getTotalProduk() {
        String sql = "SELECT COUNT(*) FROM produk";
        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error getTotalProduk: " + e.getMessage());
        }
        return 0;
    }

public static BigDecimal getTotalPenjualanHariIni() {
    List<Penjualan> daftarPenjualanHariIni = new ArrayList<>();
    
    // Pelanggan dummy untuk memenuhi syarat konstruktor Penjualan(id, Pelanggan)
    Pelanggan pelangganUmum = new Pelanggan("C001", "Umum", "-", "-", "Umum");

    String sqlPenjualan = "SELECT kode_penjualan FROM penjualan WHERE DATE(tanggal) = CURDATE()";
    String sqlDetail = "SELECT d.kode_produk, pr.nama_produk, pr.harga_beli, d.harga_jual, d.jumlah " +
                       "FROM detail_penjualan d " +
                       "JOIN produk pr ON d.kode_produk = pr.kode_produk " +
                       "WHERE d.kode_penjualan = ?";

    try (Connection conn = Koneksi.getConnection();
         PreparedStatement psPenjualan = conn.prepareStatement(sqlPenjualan);
         ResultSet rsPenjualan = psPenjualan.executeQuery()) {

        while (rsPenjualan.next()) {
            String kodePenjualan = rsPenjualan.getString("kode_penjualan");
            
            // 1. Buat objek Penjualan menggunakan konstruktor asli dosen
            Penjualan p = new Penjualan(kodePenjualan, pelangganUmum);

            // 2. Muat item detail transaksi agar hitungTotal() dosen bisa menghitungnya
            try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {
                psDetail.setString(1, kodePenjualan);
                try (ResultSet rsDetail = psDetail.executeQuery()) {
                    while (rsDetail.next()) {
                        // Buat objek Produk sementara dengan stok aman (999999) agar tambahPenjualan tidak melempar error stok
                        Produk prodTemp = new Produk(
                            rsDetail.getString("kode_produk"),
                            rsDetail.getString("nama_produk"),
                            BigDecimal.valueOf(rsDetail.getLong("harga_beli")),
                            BigDecimal.valueOf(rsDetail.getLong("harga_jual")),
                            999999
                        );
                        int jumlah = rsDetail.getInt("jumlah");
                        
                        // Panggil method asli dosen untuk mengisi detail penjualan
                        p.tambahPenjualan(prodTemp, jumlah, "SYSTEM", "Load DB");
                    }
                }
            }
            daftarPenjualanHariIni.add(p);
        }
    } catch (SQLException e) {
        System.out.println("Error getTotalPenjualanHariIni: " + e.getMessage());
    }

    // 3. Jumlahkan total menggunakan method hitungTotal() asli dari class Penjualan dosen
    BigDecimal totalHariIni = BigDecimal.ZERO;
    for (Penjualan p : daftarPenjualanHariIni) {
        totalHariIni = totalHariIni.add(p.hitungTotal());
    }

    return totalHariIni;
}

    public static int getStokMenipis(int batasMinimum) {
        String sql = "SELECT COUNT(*) FROM produk WHERE stok <= ?";
        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batasMinimum);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getStokMenipis: " + e.getMessage());
        }
        return 0;
    }
    
    
}