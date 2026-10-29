package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ActivityRepository {

    private final JdbcTemplate jdbc;

    public ActivityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ActivityRow> activities() {
        return jdbc.query("""
                SELECT ThoiGian, NguoiThucHien, HanhDong, ChiTiet
                FROM LichSuHoatDong ORDER BY ThoiGian DESC LIMIT 500
                """, (rs, row) -> new ActivityRow(rs.getObject("ThoiGian"), rs.getString("NguoiThucHien"),
                rs.getString("HanhDong"), rs.getString("ChiTiet")));
    }

    public List<LoanHistoryRow> loanHistory() {
        return jdbc.query("""
                SELECT ct.MaBanSach, ds.TenSach, dg.HoTen AS HoTenDocGia,
                       pm.NgayMuon, pm.NgayHenTra, ct.NgayTraThucTe, ct.TinhTrangSach
                FROM ChiTietMuonTra ct
                JOIN PhieuMuon pm ON pm.MaPhieuMuon = ct.MaPhieuMuon
                JOIN BanSach bs ON bs.MaBanSach = ct.MaBanSach
                JOIN DauSach ds ON ds.MaDauSach = bs.MaDauSach
                JOIN TheThuVien tv ON tv.SoThe = pm.SoThe
                JOIN DocGia dg ON dg.MaDocGia = tv.MaDocGia
                ORDER BY pm.NgayMuon DESC LIMIT 50
                """, (rs, row) -> new LoanHistoryRow(rs.getString("MaBanSach"), rs.getString("TenSach"),
                rs.getString("HoTenDocGia"), rs.getObject("NgayMuon"), rs.getObject("NgayHenTra"),
                rs.getObject("NgayTraThucTe"), rs.getString("TinhTrangSach")));
    }

    public List<LiquidatedRow> liquidatedBooks() {
        return jdbc.query("""
                SELECT bs.MaBanSach, ds.TenSach
                FROM BanSach bs JOIN DauSach ds ON ds.MaDauSach = bs.MaDauSach
                WHERE bs.TinhTrang = 3 ORDER BY bs.MaBanSach DESC
                """, (rs, row) -> new LiquidatedRow(rs.getString("MaBanSach"), rs.getString("TenSach")));
    }

    public record ActivityRow(Object time, String username, String action, String details) {}
    public record LoanHistoryRow(String barcode, String title, String reader, Object borrowedAt,
                                 Object dueAt, Object returnedAt, String condition) {}
    public record LiquidatedRow(String barcode, String title) {}
}
