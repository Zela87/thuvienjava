package com.thuvien.service;

import com.thuvien.web.SessionUser;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AccountService {

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void registerReader(String username, String password, String fullName,
                              String nationalId, LocalDate dateOfBirth, String address) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM TaiKhoan WHERE TenDangNhap = ?", Integer.class, username);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        Integer roleId = jdbc.queryForObject(
                "SELECT MaNhom FROM NhomNguoiDung WHERE TenNhom = ? LIMIT 1", Integer.class, "DocGia");
        if (roleId == null) {
            throw new IllegalStateException("Chưa cấu hình nhóm người dùng DocGia.");
        }

        jdbc.update("INSERT INTO TaiKhoan (TenDangNhap, MatKhau, MaNhom, TrangThai) VALUES (?, ?, ?, 1)",
                username, passwordEncoder.encode(password), roleId);
        jdbc.update("""
                INSERT INTO DocGia (HoTen, NgaySinh, DiaChi, CMND_CCCD, TenDangNhap)
                VALUES (?, ?, ?, ?, ?)
                """, fullName, dateOfBirth, address, nationalId, username);
    }

    public SessionUser authenticate(String username, String password) {
        String sql = """
                SELECT t.TenDangNhap, t.MatKhau, n.TenNhom,
                       COALESCE(d.HoTen, nv.HoTen, t.TenDangNhap) AS HoTen
                FROM TaiKhoan t
                JOIN NhomNguoiDung n ON n.MaNhom = t.MaNhom
                LEFT JOIN DocGia d ON d.TenDangNhap = t.TenDangNhap
                LEFT JOIN NhanVien nv ON nv.TenDangNhap = t.TenDangNhap
                WHERE t.TenDangNhap = ? AND t.TrangThai = 1
                LIMIT 1
                """;
        try {
            AccountRow account = jdbc.queryForObject(sql, (rs, rowNum) -> new AccountRow(
                    rs.getString("TenDangNhap"), rs.getString("MatKhau"),
                    rs.getString("TenNhom"), rs.getString("HoTen")), username);
            if (account == null || !matches(password, account.password())) {
                return null;
            }
            return new SessionUser(account.username(), account.fullName(), account.role());
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String stored;
        try {
            stored = jdbc.queryForObject("SELECT MatKhau FROM TaiKhoan WHERE TenDangNhap = ?",
                    String.class, username);
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
        if (!matches(oldPassword, stored)) return false;
        return jdbc.update("UPDATE TaiKhoan SET MatKhau = ? WHERE TenDangNhap = ?",
                passwordEncoder.encode(newPassword), username) == 1;
    }

    private boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return isBcrypt(stored) ? passwordEncoder.matches(raw, stored) : raw.equals(stored);
    }

    private boolean isBcrypt(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private record AccountRow(String username, String password, String role, String fullName) {}
}
