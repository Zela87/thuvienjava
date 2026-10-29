package com.thuvien.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanManagementService {

    private final JdbcTemplate jdbc;

    public LoanManagementService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ActiveLoan> activeLoans(String keyword, String filter) {
        String search = keyword == null ? "" : keyword.trim();
        StringBuilder sql = new StringBuilder("""
                SELECT pm.MaPhieuMuon, pm.SoThe, pm.NgayHenTra, doc.HoTen, ds.TenSach,
                       ct.MaBanSach, ds.GiaSach
                FROM PhieuMuon pm
                JOIN ChiTietMuonTra ct ON ct.MaPhieuMuon = pm.MaPhieuMuon
                JOIN BanSach bs ON bs.MaBanSach = ct.MaBanSach
                JOIN DauSach ds ON ds.MaDauSach = bs.MaDauSach
                JOIN TheThuVien tv ON tv.SoThe = pm.SoThe
                JOIN DocGia doc ON doc.MaDocGia = tv.MaDocGia
                WHERE ct.NgayTraThucTe IS NULL
                """);
        if (!search.isEmpty()) sql.append(" AND (pm.SoThe LIKE ? OR ds.TenSach LIKE ? OR doc.HoTen LIKE ? OR ct.MaBanSach LIKE ?)");
        if ("late".equals(filter)) sql.append(" AND pm.NgayHenTra < CURDATE()");
        if ("valid".equals(filter)) sql.append(" AND pm.NgayHenTra >= CURDATE()");
        sql.append(" ORDER BY pm.NgayHenTra, pm.MaPhieuMuon");
        var mapper = (org.springframework.jdbc.core.RowMapper<ActiveLoan>) (rs, row) -> new ActiveLoan(
                rs.getString("MaPhieuMuon"), rs.getString("SoThe"), rs.getString("HoTen"),
                rs.getString("TenSach"), rs.getString("MaBanSach"), rs.getDate("NgayHenTra").toLocalDate(),
                rs.getBigDecimal("GiaSach"));
        if (search.isEmpty()) return jdbc.query(sql.toString(), mapper);
        String like = "%" + search + "%";
        return jdbc.query(sql.toString(), mapper, like, like, like, like);
    }

    @Transactional
    public ReturnResult returnCopy(String barcode, String condition, String note, String staffUsername) {
        String normalizedCondition = condition == null ? "" : condition;
        if (!List.of("BinhThuong", "Mat", "Hong", "Rach").contains(normalizedCondition)) {
            throw new IllegalArgumentException("Tình trạng sách không hợp lệ.");
        }
        List<ReturnInfo> results = jdbc.query("""
                SELECT ds.GiaSach, pm.NgayHenTra
                FROM BanSach bs
                JOIN DauSach ds ON ds.MaDauSach = bs.MaDauSach
                JOIN ChiTietMuonTra ct ON ct.MaBanSach = bs.MaBanSach
                JOIN PhieuMuon pm ON pm.MaPhieuMuon = ct.MaPhieuMuon
                WHERE bs.MaBanSach = ? AND ct.NgayTraThucTe IS NULL
                FOR UPDATE
                """, (rs, row) -> new ReturnInfo(rs.getBigDecimal("GiaSach"), rs.getDate("NgayHenTra").toLocalDate()), barcode);
        if (results.isEmpty()) throw new IllegalArgumentException("Bản sách không có phiếu mượn đang mở.");
        ReturnInfo info = results.getFirst();
        BigDecimal price = info.price() == null ? BigDecimal.ZERO : info.price();
        BigDecimal damageFine = BigDecimal.ZERO;
        int resultingStatus = 1;
        String physical = "Bình thường";
        switch (normalizedCondition) {
            case "Mat" -> { damageFine = price; resultingStatus = 2; physical = "Mất"; }
            case "Hong" -> { damageFine = price.multiply(new BigDecimal("0.5")); resultingStatus = 3; physical = "Hỏng"; }
            case "Rach" -> { damageFine = price.multiply(new BigDecimal("0.2")); physical = "Rách"; }
        }
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(info.dueDate(), today));
        BigDecimal overdueFine = price.multiply(new BigDecimal("0.1")).multiply(BigDecimal.valueOf(lateDays));
        BigDecimal totalFine = damageFine.add(overdueFine).setScale(2, RoundingMode.HALF_UP);
        String statusText = physical;
        if (lateDays > 0) {
            String lateText = "Muộn " + lateDays + " ngày (Phạt " + overdueFine.setScale(0, RoundingMode.HALF_UP) + "đ)";
            statusText = "Bình thường".equals(physical) ? lateText : lateText + " - " + physical;
        }
        int changed = jdbc.update("""
                UPDATE ChiTietMuonTra
                SET NgayTraThucTe = CURDATE(), TinhTrangSach = ?, TienPhat = ?, GhiChu = ?
                WHERE MaBanSach = ? AND NgayTraThucTe IS NULL
                """, statusText, totalFine, note == null ? "" : note.trim(), barcode);
        if (changed != 1) throw new IllegalArgumentException("Phiếu mượn đã được trả trước đó.");

        String history = Timestamp.valueOf(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                + " - Trạng thái: " + statusText + (note == null || note.isBlank() ? "" : " (Ghi chú: " + note.trim() + ")") + "\n";
        jdbc.update("""
                UPDATE BanSach
                SET TinhTrang = ?, TinhTrangVatLy = ?, GhiChuTinhTrang = ?,
                    LichSuTinhTrang = CONCAT(IFNULL(LichSuTinhTrang, ''), ?)
                WHERE MaBanSach = ?
                """, resultingStatus, statusText, note == null ? "" : note.trim(), history, barcode);
        jdbc.update("""
                INSERT INTO LichSuHoatDong (NguoiThucHien, HanhDong, ChiTiet)
                VALUES (?, ?, ?)
                """, staffUsername, "Trả sách " + barcode,
                "Tình trạng: " + statusText + "; tiền phạt: " + totalFine.toPlainString());
        return new ReturnResult(barcode, statusText, totalFine, lateDays);
    }

    public record ActiveLoan(String loanId, String cardNumber, String readerName, String title,
                             String barcode, LocalDate dueDate, BigDecimal price) {}
    private record ReturnInfo(BigDecimal price, LocalDate dueDate) {}
    public record ReturnResult(String barcode, String condition, BigDecimal fine, long overdueDays) {}
}
