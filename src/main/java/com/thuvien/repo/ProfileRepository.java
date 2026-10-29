package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ProfileRepository {

    private final JdbcTemplate jdbc;

    public ProfileRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Profile profile(String username, boolean reader) {
        String table = reader ? "DocGia" : "NhanVien";
        List<Profile> rows = jdbc.query("SELECT HoTen, CMND_CCCD, NgaySinh, DiaChi FROM " + table + " WHERE TenDangNhap = ?",
                (rs, row) -> new Profile(rs.getString("HoTen"), rs.getString("CMND_CCCD"),
                        rs.getObject("NgaySinh", LocalDate.class), rs.getString("DiaChi")), username);
        return rows.isEmpty() ? new Profile("", "", null, "") : rows.get(0);
    }

    public void update(String username, boolean reader, String name, String nationalId,
                       LocalDate dateOfBirth, String address) {
        String table = reader ? "DocGia" : "NhanVien";
        jdbc.update("UPDATE " + table + " SET HoTen = ?, CMND_CCCD = ?, NgaySinh = ?, DiaChi = ? WHERE TenDangNhap = ?",
                name.trim(), nationalId == null ? "" : nationalId.trim(), dateOfBirth,
                address == null ? "" : address.trim(), username);
    }

    public record Profile(String name, String nationalId, LocalDate dateOfBirth, String address) {}
}
