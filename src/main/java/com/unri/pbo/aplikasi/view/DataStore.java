/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


package com.unri.pbo.aplikasi.view;

import com.unri.pbo.Produk;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
        listProduk.add(p);
        mapKategori.put(p.getIdProduk(), kategori);
    }
}