package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public class ReaderRepository {

    private final JdbcTemplate jdbc;

    public ReaderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ReaderRow> readers(String keyword) {
        String search = keyword == null ? "" : keyword.trim();
        String sql = """
                SELECT d.MaDocGia, d.HoTen, d.TenDangNhap, d.CMND_CCCD, d.NgaySinh, d.DiaChi,
                       t.SoThe, t.NgayCap, t.NgayHetHan, t.TienDatCoc, t.TrangThai
                FROM DocGia d LEFT JOIN TheThuVien t ON t.MaDocGia = d.MaDocGia
                WHERE (? = '' OR d.HoTen LIKE ? OR d.TenDangNhap LIKE ? OR d.CMND_CCCD LIKE ? OR d.MaDocGia LIKE ?)
                ORDER BY d.MaDocGia DESC
                """;
        String like = "%" + search + "%";
        return jdbc.query(sql, (rs, row) -> {
            int statusValue = rs.getInt("TrangThai");
            Integer status = rs.wasNull() ? null : statusValue;
            return new ReaderRow(rs.getString("MaDocGia"), rs.getString("HoTen"),
                    rs.getString("TenDangNhap"), rs.getString("CMND_CCCD"), rs.getObject("NgaySinh"),
                    rs.getString("DiaChi"), rs.getString("SoThe"), rs.getObject("NgayCap"),
                    rs.getObject("NgayHetHan"), rs.getBigDecimal("TienDatCoc"), status);
        }, search, like, like, like, like);
    }

    public List<ReaderOption> readersWithoutCard() {
        return jdbc.query("""
                SELECT d.MaDocGia, d.HoTen FROM DocGia d
                WHERE NOT EXISTS (SELECT 1 FROM TheThuVien t WHERE t.MaDocGia = d.MaDocGia)
                ORDER BY d.HoTen
                """, (rs, row) -> new ReaderOption(rs.getString("MaDocGia"), rs.getString("HoTen")));
    }

    public void issueCard(String readerId, BigDecimal deposit) {
        if (deposit == null || deposit.signum() < 0) throw new IllegalArgumentException("Tiền đặt cọc không hợp lệ.");
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM DocGia WHERE MaDocGia = ?", Integer.class, readerId);
        if (exists == null || exists == 0) throw new IllegalArgumentException("Không tìm thấy độc giả.");
        Integer hasCard = jdbc.queryForObject("SELECT COUNT(*) FROM TheThuVien WHERE MaDocGia = ?", Integer.class, readerId);
        if (hasCard != null && hasCard > 0) throw new IllegalArgumentException("Độc giả đã có thẻ.");
        String cardNumber = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        jdbc.update("""
                INSERT INTO TheThuVien (SoThe, MaDocGia, NgayCap, NgayHetHan, TienDatCoc, TrangThai)
                VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 YEAR), ?, 1)
                """, cardNumber, readerId, deposit);
    }

    public void setCardStatus(String cardNumber, int status) {
        if (status != 0 && status != 1) throw new IllegalArgumentException("Trạng thái thẻ không hợp lệ.");
        jdbc.update("UPDATE TheThuVien SET TrangThai = ? WHERE SoThe = ?", status, cardNumber);
    }

    public void renewCard(String cardNumber) {
        jdbc.update("UPDATE TheThuVien SET NgayHetHan = DATE_ADD(NgayHetHan, INTERVAL 1 YEAR) WHERE SoThe = ?", cardNumber);
    }

    public record ReaderRow(String id, String fullName, String username, String nationalId, Object dateOfBirth,
                            String address, String cardNumber, Object issuedAt, Object expiresAt,
                            BigDecimal deposit, Integer cardStatus) {}
    public record ReaderOption(String id, String fullName) {}
}
