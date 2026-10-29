package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountAdminRepository {

    private final JdbcTemplate jdbc;

    public AccountAdminRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AccountRow> accounts() {
        return jdbc.query("""
                SELECT tk.TenDangNhap, nd.TenNhom, tk.TrangThai,
                       COALESCE(dg.HoTen, nv.HoTen) AS HoTen
                FROM TaiKhoan tk
                JOIN NhomNguoiDung nd ON nd.MaNhom = tk.MaNhom
                LEFT JOIN DocGia dg ON dg.TenDangNhap = tk.TenDangNhap
                LEFT JOIN NhanVien nv ON nv.TenDangNhap = tk.TenDangNhap
                ORDER BY tk.MaNhom, tk.TenDangNhap
                """, (rs, row) -> new AccountRow(rs.getString("TenDangNhap"), rs.getString("TenNhom"),
                rs.getInt("TrangThai"), rs.getString("HoTen")));
    }

    public void create(String username, String passwordHash, String name, int roleId, String role) {
        jdbc.update("INSERT INTO TaiKhoan (TenDangNhap, MatKhau, MaNhom, TrangThai) VALUES (?, ?, ?, 1)",
                username, passwordHash, roleId);
        if ("DocGia".equals(role)) {
            jdbc.update("INSERT INTO DocGia (HoTen, TenDangNhap) VALUES (?, ?)", name, username);
        } else {
            jdbc.update("INSERT INTO NhanVien (HoTen, TenDangNhap) VALUES (?, ?)", name, username);
        }
    }

    public void setStatus(String username, int status) {
        jdbc.update("UPDATE TaiKhoan SET TrangThai = ? WHERE TenDangNhap = ?", status, username);
    }

    public record AccountRow(String username, String role, int status, String name) {}
}
