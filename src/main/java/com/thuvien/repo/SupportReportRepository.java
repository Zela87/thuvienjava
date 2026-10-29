package com.thuvien.repo;

import com.thuvien.web.SessionUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class SupportReportRepository {

    private final JdbcTemplate jdbc;

    public SupportReportRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<SupportReport> reports(SessionUser user) {
        StringBuilder sql = new StringBuilder("""
                SELECT bc.MaBaoCao, bc.NguoiBaoCao, bc.TieuDe, bc.LoaiSuCo, bc.NoiDung,
                       bc.TrangThai, bc.PhanHoi, bc.NgayBaoCao, tv.SoThe
                FROM BaoCaoSuCo bc
                LEFT JOIN DocGia dg ON dg.TenDangNhap = bc.NguoiBaoCao
                LEFT JOIN TheThuVien tv ON tv.MaDocGia = dg.MaDocGia
                WHERE 1 = 1
                """);
        if (user.isAdmin()) {
            sql.append(" AND bc.LoaiSuCo IN ('Web lỗi', 'Khác')");
        } else if (user.isStaff()) {
            sql.append(" AND bc.LoaiSuCo IN ('Vấn đề về thẻ', 'Hết sách', 'Khác')");
        } else {
            sql.append(" AND bc.NguoiBaoCao = ?");
        }
        sql.append(" ORDER BY bc.NgayBaoCao DESC");
        var mapper = (org.springframework.jdbc.core.RowMapper<SupportReport>) (rs, row) -> new SupportReport(
                rs.getString("MaBaoCao"), rs.getString("NguoiBaoCao"), rs.getString("TieuDe"),
                rs.getString("LoaiSuCo"), rs.getString("NoiDung"), rs.getString("TrangThai"),
                rs.getString("PhanHoi"), rs.getObject("NgayBaoCao"), rs.getString("SoThe"));
        return user.isStaff() ? jdbc.query(sql.toString(), mapper) : jdbc.query(sql.toString(), mapper, user.getUsername());
    }

    public void submit(String username, String type, String title, String body) {
        if (!List.of("Vấn đề về thẻ", "Hết sách", "Web lỗi", "Khác").contains(type)) {
            throw new IllegalArgumentException("Loại sự cố không hợp lệ.");
        }
        jdbc.update("""
                INSERT INTO BaoCaoSuCo (NguoiBaoCao, TieuDe, LoaiSuCo, NoiDung, TrangThai)
                VALUES (?, ?, ?, ?, 'Chờ xử lý')
                """, username, title.trim(), type, body.trim());
    }

    @Transactional
    public void reply(String username, String reportId, String reply) {
        int updated = jdbc.update("UPDATE BaoCaoSuCo SET PhanHoi = ?, TrangThai = 'Đã xong' WHERE MaBaoCao = ?",
                reply.trim(), reportId);
        if (updated != 1) throw new IllegalArgumentException("Không tìm thấy báo cáo.");
        jdbc.update("INSERT INTO LichSuHoatDong (NguoiThucHien, HanhDong, ChiTiet) VALUES (?, ?, ?)",
                username, "Phản hồi hỗ trợ", "Mã BC: " + reportId);
    }

    public record SupportReport(String id, String reporter, String title, String type, String body,
                                String status, String reply, Object reportedAt, String cardNumber) {}
}
