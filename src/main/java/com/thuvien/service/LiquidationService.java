package com.thuvien.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LiquidationService {

    private final JdbcTemplate jdbc;

    public LiquidationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<LiquidationCandidate> candidates() {
        return jdbc.query("""
                SELECT b.MaBanSach, d.TenSach, b.TinhTrang, b.TinhTrangVatLy
                FROM BanSach b JOIN DauSach d ON d.MaDauSach = b.MaDauSach
                WHERE b.TinhTrang IN (1, 2)
                ORDER BY b.TinhTrang DESC, d.TenSach
                """, (rs, row) -> new LiquidationCandidate(rs.getString("MaBanSach"),
                rs.getString("TenSach"), rs.getInt("TinhTrang"), rs.getString("TinhTrangVatLy")));
    }

    @Transactional
    public void liquidate(String barcode, String reason, String username) {
        List<Integer> states = jdbc.query("SELECT TinhTrang FROM BanSach WHERE MaBanSach = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), barcode);
        if (states.isEmpty()) throw new IllegalArgumentException("Không tìm thấy bản sách.");
        int state = states.get(0);
        if (state == 0) throw new IllegalArgumentException("Sách đang được mượn, không thể thanh lý.");
        if (state == 3) throw new IllegalArgumentException("Bản sách đã thanh lý.");
        if (state != 1 && state != 2) throw new IllegalArgumentException("Trạng thái bản sách không cho phép thanh lý.");
        jdbc.update("INSERT INTO LichSuHoatDong (NguoiThucHien, HanhDong, ChiTiet) VALUES (?, ?, ?)",
                username, "Thanh lý sách", "Mã: " + barcode + ". Lý do: " + reason.trim());
        int updated = jdbc.update("UPDATE BanSach SET TinhTrang = 3 WHERE MaBanSach = ? AND TinhTrang = ?", barcode, state);
        if (updated != 1) throw new IllegalArgumentException("Trạng thái sách vừa thay đổi, hãy tải lại trang.");
    }

    public record LiquidationCandidate(String barcode, String title, int status, String physicalCondition) {}
}
