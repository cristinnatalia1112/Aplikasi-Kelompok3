/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.unri.pbo.aplikasi.view;
import com.unri.pbo.DetailPenjualan;
import com.unri.pbo.Pelanggan;
import com.unri.pbo.Penjualan;
import com.unri.pbo.Produk;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
/**
 *
 * @author M S I
 */
public class transaksiPenjualan extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(transaksiPenjualan.class.getName());

    /**
     * Creates new form transaksiPenjualan
     */
    
private static final String PENGGUNA = "Kasir";
private Penjualan penjualan;
private DefaultTableModel model;
private final NumberFormat rupiah = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
private final List<Pelanggan> daftarPelanggan = new ArrayList<>();
private final List<Produk> daftarProduk = new ArrayList<>();

private void loadPelangganFromDB() {
    daftarPelanggan.clear();
    String sql = "SELECT kode_pelanggan, nama, telepon, alamat FROM pelanggan ORDER BY kode_pelanggan ASC";
    try (Connection conn = Koneksi.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            String kode = rs.getString("kode_pelanggan");
            String nama = rs.getString("nama");
            String telp = rs.getString("telepon") != null ? rs.getString("telepon") : "-";
            String alamat = rs.getString("alamat") != null ? rs.getString("alamat") : "-";
            daftarPelanggan.add(new Pelanggan(kode, nama, telp, alamat, "Pelanggan"));
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Gagal memuat pelanggan: " + ex.getMessage());
    }

    if (daftarPelanggan.isEmpty()) {
        daftarPelanggan.add(new Pelanggan("C001", "Umum", "-", "-", "Umum"));
    }
}

private void loadProdukFromDB() {
    daftarProduk.clear();
    String sql = "SELECT kode_produk, nama_produk, harga_beli, harga_jual, stok FROM produk ORDER BY kode_produk ASC";
    try (Connection conn = Koneksi.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            daftarProduk.add(new Produk(
                rs.getString("kode_produk"),
                rs.getString("nama_produk"),
                BigDecimal.valueOf(rs.getLong("harga_beli")),
                BigDecimal.valueOf(rs.getLong("harga_jual")),
                rs.getInt("stok")
            ));
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Gagal memuat produk: " + ex.getMessage());
    }
}
private void simpanTransaksiKeDB(String kodePenjualan) throws SQLException {
    String sqlCariPelanggan = "SELECT kode_pelanggan FROM pelanggan WHERE nama = ? LIMIT 1";
    String sqlPenjualan = "INSERT INTO penjualan (kode_penjualan, tanggal, kode_pelanggan, total) VALUES (?, NOW(), ?, ?)";
    String sqlDetail = "INSERT INTO detail_penjualan (kode_penjualan, kode_produk, jumlah, harga_jual, subtotal) VALUES (?, ?, ?, ?, ?)";
    String sqlStok = "UPDATE produk SET stok = stok - ? WHERE kode_produk = ? AND stok >= ?";
    String sqlTransaksi = "INSERT INTO transaksi (kode, tanggal, jenis, keterangan, total) VALUES (?, CURDATE(), 'Penjualan', ?, ?)";

    Pelanggan pelangganDipilih = daftarPelanggan.get(cbPelanggan.getSelectedIndex());
    long totalBayar = penjualan.hitungTotal().longValue();

    Connection conn = null;
    try {
        conn = Koneksi.getConnection();
        conn.setAutoCommit(false);

        String kodePelanggan = "C001";
        try (PreparedStatement psPlg = conn.prepareStatement(sqlCariPelanggan)) {
            psPlg.setString(1, pelangganDipilih.getNama());
            try (ResultSet rsPlg = psPlg.executeQuery()) {
                if (rsPlg.next()) {
                    kodePelanggan = rsPlg.getString("kode_pelanggan");
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlPenjualan)) {
            ps.setString(1, kodePenjualan);
            ps.setString(2, kodePelanggan);
            ps.setLong(3, totalBayar);
            ps.executeUpdate();
        }

        StringBuilder keterangan = new StringBuilder();

        for (Object o : penjualan.getDaftarDetail()) {
            DetailPenjualan d = (DetailPenjualan) o;
            String namaProduk = d.getProduk().getNamaProduk();
            int jumlah = d.getJumlah();
            long hargaJual = d.getHargaJual().longValue();
            long subtotal = d.getSubtotal().longValue();

            String kodeProduk = d.getProduk().getIdProduk();

            try (PreparedStatement psDet = conn.prepareStatement(sqlDetail)) {
                psDet.setString(1, kodePenjualan);
                psDet.setString(2, kodeProduk);
                psDet.setInt(3, jumlah);
                psDet.setLong(4, hargaJual);
                psDet.setLong(5, subtotal);
                psDet.executeUpdate();
            }

            try (PreparedStatement psUpd = conn.prepareStatement(sqlStok)) {
                psUpd.setInt(1, jumlah);
                psUpd.setString(2, kodeProduk);
                psUpd.setInt(3, jumlah);
                int affected = psUpd.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Stok tidak cukup di database untuk: " + namaProduk);
                }
            }

            if (keterangan.length() > 0) keterangan.append(", ");
            keterangan.append(namaProduk).append(" x").append(jumlah);
        }

        try (PreparedStatement psTrx = conn.prepareStatement(sqlTransaksi)) {
            psTrx.setString(1, kodePenjualan);
            psTrx.setString(2, keterangan.toString());
            psTrx.setLong(3, totalBayar);
            psTrx.executeUpdate();
        }

        conn.commit();
    } catch (SQLException ex) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
        throw ex;
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {}
        }
    }
}
private String generateIdPenjualanFromDB() {
    String sql = "SELECT kode_penjualan FROM penjualan ORDER BY kode_penjualan DESC LIMIT 1";
    try (Connection conn = Koneksi.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        int next = 1;
        if (rs.next()) {
            String terakhir = rs.getString("kode_penjualan");
            String angka = terakhir.replaceAll("[^0-9]", "");
            if (!angka.isEmpty()) {
                next = Integer.parseInt(angka) + 1;
            }
        }
        return String.format("PJ%04d", next);
    } catch (SQLException ex) {
        return "PJ0001";
    }
}

public transaksiPenjualan() {
    initComponents();
    rupiah.setMaximumFractionDigits(0);
    model = (DefaultTableModel) tblkeranjang.getModel();
    tblkeranjang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    loadPelangganFromDB();
    loadProdukFromDB();
    isiPelanggan();
    isiProduk();
    transaksiBaru();

    setPreferredSize(new java.awt.Dimension(1000, 600));
    pack();
    setLocationRelativeTo(null);

    javax.swing.table.DefaultTableCellRenderer hr = new javax.swing.table.DefaultTableCellRenderer();
    hr.setOpaque(true);
    hr.setBackground(new java.awt.Color(0, 0, 102));
    hr.setForeground(java.awt.Color.WHITE);
    hr.setFont(hr.getFont().deriveFont(java.awt.Font.BOLD));
    tblkeranjang.getTableHeader().setDefaultRenderer(hr);
}
private void isiPelanggan() {
    cbPelanggan.removeAllItems();
    for (Pelanggan p : daftarPelanggan) {
        cbPelanggan.addItem(p.getNama());
    }
}

private void isiProduk() {
    int pilih = cbProduk.getSelectedIndex();
    cbProduk.removeAllItems();
    for (Produk p : daftarProduk) {
        cbProduk.addItem(p.getNamaProduk() + " - " + rupiah.format(p.getHargaJual()) + " (stok " + p.getStok() + ")");
    }
    if (pilih >= 0 && pilih < daftarProduk.size()) cbProduk.setSelectedIndex(pilih);
}

private void transaksiBaru() {
    String id = generateIdPenjualanFromDB();
    penjualan = new Penjualan(id, daftarPelanggan.get(cbPelanggan.getSelectedIndex()));
    lblId.setText(id);
    refreshTabel();
}

private void refreshTabel() {
    model.setRowCount(0);
    for (Object o : penjualan.getDaftarDetail()) {
        DetailPenjualan d = (DetailPenjualan) o;
        model.addRow(new Object[]{d.getIdDetailPenjualan(), d.getProduk().getNamaProduk(),
            rupiah.format(d.getHargaJual()), d.getJumlah(), rupiah.format(d.getSubtotal())});
    }
    lbltotal.setText(rupiah.format(penjualan.hitungTotal()));
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        panelAtas = new javax.swing.JPanel();
        lblIdTitle = new javax.swing.JLabel();
        lblId = new javax.swing.JLabel();
        lblPelangganTitle = new javax.swing.JLabel();
        cbPelanggan = new javax.swing.JComboBox<>();
        lblProdukTitle = new javax.swing.JLabel();
        cbProduk = new javax.swing.JComboBox<>();
        lblJumlahTitle = new javax.swing.JLabel();
        spjumlah = new javax.swing.JSpinner();
        btnTambahItem = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblkeranjang = new javax.swing.JTable();
        btnUbahJumlah = new javax.swing.JButton();
        btnHapusItem = new javax.swing.JButton();
        lblTotalTitle = new javax.swing.JLabel();
        lbltotal = new javax.swing.JLabel();
        btnBayar = new javax.swing.JButton();
        btnBatalkan = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("POS TOKO BERKAT - Transaksi Penjualan");
        setResizable(false);
        getContentPane().setLayout(null);

        jPanel1.setBackground(new java.awt.Color(0, 0, 102));
        jPanel1.setToolTipText("");
        jPanel1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel1.setLayout(null);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("POS TOKO BERKAT");
        jLabel1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel1.add(jLabel1);
        jLabel1.setBounds(70, 30, 171, 25);

        jButton1.setText("Dashboard");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);
        jButton1.setBounds(20, 90, 240, 40);

        jButton2.setText("Data Produk");
        jPanel1.add(jButton2);
        jButton2.setBounds(20, 160, 240, 40);

        jButton3.setText("Pelanggan & Supplier");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton3);
        jButton3.setBounds(20, 230, 240, 40);

        jButton4.setBackground(new java.awt.Color(30, 58, 138));
        jButton4.setText("Kasir");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton4);
        jButton4.setBounds(20, 300, 240, 40);

        jButton5.setText("Restock");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton5);
        jButton5.setBounds(20, 370, 240, 40);

        jButton6.setText("Riwayat Transaksi");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton6);
        jButton6.setBounds(20, 440, 240, 40);

        jPanel2.setBackground(new java.awt.Color(204, 204, 204));
        jPanel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jPanel2.setInheritsPopupMenu(true);
        jPanel2.setLayout(null);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Transaksi Penjualan");
        jPanel2.add(jLabel5);
        jLabel5.setBounds(24, 24, 220, 25);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 2, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(102, 102, 102));
        jLabel6.setText("Catat penjualan kasir Toko Berkat");
        jPanel2.add(jLabel6);
        jLabel6.setBounds(24, 55, 190, 16);

        panelAtas.setBackground(new java.awt.Color(255, 255, 255));
        panelAtas.setLayout(null);

        lblIdTitle.setText("ID Penjualan");
        panelAtas.add(lblIdTitle);
        lblIdTitle.setBounds(15, 10, 190, 20);
        panelAtas.add(lblId);
        lblId.setBounds(15, 32, 190, 25);

        lblPelangganTitle.setText("Pelanggan");
        panelAtas.add(lblPelangganTitle);
        lblPelangganTitle.setBounds(225, 10, 190, 20);

        cbPelanggan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbPelangganActionPerformed(evt);
            }
        });
        panelAtas.add(cbPelanggan);
        cbPelanggan.setBounds(225, 32, 190, 25);

        lblProdukTitle.setText("Produk");
        panelAtas.add(lblProdukTitle);
        lblProdukTitle.setBounds(15, 66, 190, 20);

        panelAtas.add(cbProduk);
        cbProduk.setBounds(15, 88, 320, 25);

        lblJumlahTitle.setText("Jumlah");
        panelAtas.add(lblJumlahTitle);
        lblJumlahTitle.setBounds(350, 66, 100, 20);

        spjumlah.setModel(new javax.swing.SpinnerNumberModel(1, 1, 9999, 1));
        panelAtas.add(spjumlah);
        spjumlah.setBounds(350, 88, 89, 25);

        btnTambahItem.setBackground(new java.awt.Color(66, 107, 214));
        btnTambahItem.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnTambahItem.setForeground(new java.awt.Color(255, 255, 255));
        btnTambahItem.setText("Tambah Item");
        btnTambahItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTambahItemActionPerformed(evt);
            }
        });
        panelAtas.add(btnTambahItem);
        btnTambahItem.setBounds(435, 86, 130, 28);

        jPanel2.add(panelAtas);
        panelAtas.setBounds(24, 84, 645, 120);

        tblkeranjang.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID Detail", "Produk", "Harga", "Jumlah", "SubTotal"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblkeranjang.setSelectionBackground(new java.awt.Color(219, 228, 255));
        jScrollPane1.setViewportView(tblkeranjang);

        jPanel2.add(jScrollPane1);
        jScrollPane1.setBounds(24, 216, 645, 230);

        btnUbahJumlah.setText("Ubah Jumlah");
        btnUbahJumlah.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUbahJumlahActionPerformed(evt);
            }
        });
        jPanel2.add(btnUbahJumlah);
        btnUbahJumlah.setBounds(24, 456, 130, 30);

        btnHapusItem.setText("Hapus Item");
        btnHapusItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusItemActionPerformed(evt);
            }
        });
        jPanel2.add(btnHapusItem);
        btnHapusItem.setBounds(164, 456, 130, 30);

        lblTotalTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblTotalTitle.setText("TOTAL:");
        jPanel2.add(lblTotalTitle);
        lblTotalTitle.setBounds(400, 456, 70, 30);

        lbltotal.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lbltotal.setText("Rp0");
        jPanel2.add(lbltotal);
        lbltotal.setBounds(475, 456, 194, 30);

        btnBayar.setBackground(new java.awt.Color(0, 0, 102));
        btnBayar.setForeground(new java.awt.Color(255, 255, 255));
        btnBayar.setText("Selesaikan Transaksi");
        btnBayar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBayarActionPerformed(evt);
            }
        });
        jPanel2.add(btnBayar);
        btnBayar.setBounds(399, 500, 170, 40);

        btnBatalkan.setText("Batalkan");
        btnBatalkan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBatalkanActionPerformed(evt);
            }
        });
        jPanel2.add(btnBatalkan);
        btnBatalkan.setBounds(579, 500, 90, 40);

        jPanel1.add(jPanel2);
        jPanel2.setBounds(280, 0, 720, 578);

        getContentPane().add(jPanel1);
        jPanel1.setBounds(10, 8, 1010, 590);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
       dispose();
    Dashboard d = new Dashboard();
    d.setPreferredSize(new java.awt.Dimension(1000, 600));
    d.pack();
    d.setLocationRelativeTo(null);
    d.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        dispose();
        new manajemenPelanggan_Supplier().setVisible(true);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        dispose();
        new riwayat().setVisible(true);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void btnTambahItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahItemActionPerformed
       try {
        int idx = cbProduk.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Pilih produk dulu.");
            return;
        }
        Produk p = daftarProduk.get(idx);
        int jumlah = (Integer) spjumlah.getValue();
        if (jumlah > p.getStok()) {
            JOptionPane.showMessageDialog(this, "Stok tidak cukup. Stok tersedia: " + p.getStok());
            return;
        }
        penjualan.tambahPenjualan(p, jumlah, PENGGUNA, "Tambah item");
        refreshTabel();
        isiProduk();
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error: " + ex, "Gagal", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnTambahItemActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        dispose();
        new transaksiPenjualan().setVisible(true);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void btnUbahJumlahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUbahJumlahActionPerformed
       int r = tblkeranjang.getSelectedRow();
    if (r < 0) {
        JOptionPane.showMessageDialog(this, "Pilih item di tabel dulu.");
        return;
    }
    String id = model.getValueAt(r, 0).toString();
    String in = JOptionPane.showInputDialog(this, "Jumlah baru (total, bukan tambahan):");
    if (in == null) return;
    try {
        int baru = Integer.parseInt(in.trim());
        if (baru <= 0) throw new NumberFormatException();
        DetailPenjualan target = null;
        for (Object o : penjualan.getDaftarDetail()) {
            DetailPenjualan d = (DetailPenjualan) o;
            if (d.getIdDetailPenjualan().equals(id)) target = d;
        }
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Item tidak ditemukan.");
            return;
        }
        int tersedia = target.getProduk().getStok() + target.getJumlah();
        if (baru > tersedia) {
            JOptionPane.showMessageDialog(this, "Stok tidak cukup. Maksimal: " + tersedia);
            return;
        }
        penjualan.ubahDetail(id, baru, PENGGUNA, "Ubah jumlah");
        refreshTabel();
        isiProduk();
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(this, "Masukkan angka bulat lebih dari 0.");
    } catch (RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnUbahJumlahActionPerformed

    private void btnHapusItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusItemActionPerformed
       int r = tblkeranjang.getSelectedRow();
    if (r < 0) {
        JOptionPane.showMessageDialog(this, "Pilih item di tabel dulu.");
        return;
    }
    String id = model.getValueAt(r, 0).toString();
    try {
        penjualan.hapusDetail(id, PENGGUNA, "Hapus item");
        refreshTabel();
        isiProduk();
    } catch (RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnHapusItemActionPerformed

    private void btnBayarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBayarActionPerformed

          if (penjualan.getDaftarDetail().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang masih kosong.");
            return;
        }

        long totalBayar = penjualan.hitungTotal().longValue();
        String inputBayar = JOptionPane.showInputDialog(
                this,
                "Total Belanja: " + rupiah.format(totalBayar) + "\nMasukkan uang bayar:",
                "Pembayaran",
                JOptionPane.QUESTION_MESSAGE
        );

        if (inputBayar == null) {
            return; // Kasir menekan Cancel
        }

        String bersih = inputBayar.replaceAll("[^0-9]", "");
        if (bersih.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nominal uang bayar tidak valid.");
            return;
        }

        long uangBayar;
        try {
            uangBayar = Long.parseLong(bersih);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Nominal uang bayar terlalu besar atau tidak valid.");
            return;
        }

        if (uangBayar < totalBayar) {
            JOptionPane.showMessageDialog(
                    this,
                    "Uang bayar kurang!\nTotal: " + rupiah.format(totalBayar)
                            + "\nKurang: " + rupiah.format(totalBayar - uangBayar),
                    "Pembayaran Kurang",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        long kembalian = uangBayar - totalBayar;

        try {
            String kodePenjualan = lblId.getText().trim();
            penjualan.finalisasiTransaksi(PENGGUNA);
            simpanTransaksiKeDB(kodePenjualan);
            penjualan.cetakNota();
            JOptionPane.showMessageDialog(
                    this,
                    "Transaksi " + kodePenjualan + " selesai.\n"
                            + "Total     : " + rupiah.format(totalBayar) + "\n"
                            + "Bayar     : " + rupiah.format(uangBayar) + "\n"
                            + "Kembalian : " + rupiah.format(kembalian),
                    "Struk / Ringkasan Pembayaran",
                    JOptionPane.INFORMATION_MESSAGE
            );
            loadProdukFromDB();
            isiProduk();
            transaksiBaru();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Gagal simpan ke SQL: " + ex.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnBayarActionPerformed

    private void btnBatalkanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBatalkanActionPerformed
      if (penjualan.getDaftarDetail().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Keranjang masih kosong, tidak ada yang dibatalkan.");
        return;
    }
    int ok = JOptionPane.showConfirmDialog(this, "Batalkan transaksi ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
    if (ok != JOptionPane.YES_OPTION) return;
    try {
        penjualan.batalkanTransaksi(PENGGUNA, "Dibatalkan kasir");
    } catch (RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
        return;
    }
    transaksiBaru();
    isiProduk();
    }//GEN-LAST:event_btnBatalkanActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
       dispose();
        new transaksiPembelian().setVisible(true);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void cbPelangganActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbPelangganActionPerformed

    }//GEN-LAST:event_cbPelangganActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new transaksiPenjualan().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBatalkan;
    private javax.swing.JButton btnBayar;
    private javax.swing.JButton btnHapusItem;
    private javax.swing.JButton btnTambahItem;
    private javax.swing.JButton btnUbahJumlah;
    private javax.swing.JComboBox<String> cbPelanggan;
    private javax.swing.JComboBox<String> cbProduk;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblId;
    private javax.swing.JLabel lblIdTitle;
    private javax.swing.JLabel lblJumlahTitle;
    private javax.swing.JLabel lblPelangganTitle;
    private javax.swing.JLabel lblProdukTitle;
    private javax.swing.JLabel lblTotalTitle;
    private javax.swing.JLabel lbltotal;
    private javax.swing.JPanel panelAtas;
    private javax.swing.JSpinner spjumlah;
    private javax.swing.JTable tblkeranjang;
    // End of variables declaration//GEN-END:variables
}
