package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class StatisticsRepository {

    private final JdbcTemplate jdbc;

    public StatisticsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Summary summary() {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM DauSach) AS TongDauSach,
                       (SELECT COUNT(*) FROM DocGia) AS TongDocGia,
                       (SELECT COUNT(*) FROM ChiTietMuonTra WHERE NgayTraThucTe IS NULL) AS DangMuon,
                       (SELECT COALESCE(SUM(TienPhat), 0) FROM ChiTietMuonTra) AS TongTienPhat,
                       (SELECT COUNT(*) FROM BanSach WHERE TinhTrang = 3) AS DaThanhLy
                """, (rs, row) -> new Summary(rs.getInt("TongDauSach"), rs.getInt("TongDocGia"),
                rs.getInt("DangMuon"), rs.getBigDecimal("TongTienPhat"), rs.getInt("DaThanhLy")));
    }

    public List<OverdueRow> overdueBooks() {
        return jdbc.query("""
                SELECT dg.HoTen, tv.SoThe, ds.TenSach, bs.MaBanSach, pm.NgayHenTra,
                       DATEDIFF(CURDATE(), pm.NgayHenTra) AS SoNgayTre
                FROM ChiTietMuonTra ct
                JOIN BanSach bs ON bs.MaBanSach = ct.MaBanSach
                JOIN DauSach ds ON ds.MaDauSach = bs.MaDauSach
                JOIN PhieuMuon pm ON pm.MaPhieuMuon = ct.MaPhieuMuon
                JOIN TheThuVien tv ON tv.SoThe = pm.SoThe
                JOIN DocGia dg ON dg.MaDocGia = tv.MaDocGia
                WHERE ct.NgayTraThucTe IS NULL AND pm.NgayHenTra < CURDATE()
                ORDER BY SoNgayTre DESC
                """, (rs, row) -> new OverdueRow(rs.getString("HoTen"), rs.getString("SoThe"),
                rs.getString("TenSach"), rs.getString("MaBanSach"), rs.getObject("NgayHenTra"), rs.getInt("SoNgayTre")));
    }

    public record Summary(int bookTitles, int readers, int currentlyBorrowed, BigDecimal fines, int liquidatedCopies) {}
    public record OverdueRow(String reader, String cardNumber, String title, String barcode, Object dueDate, int daysLate) {}
}
