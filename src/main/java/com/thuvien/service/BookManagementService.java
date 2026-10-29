package com.thuvien.service;

import com.thuvien.repo.ReferenceDataRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

@Service
public class BookManagementService {

    private final JdbcTemplate jdbc;

    public BookManagementService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BookAdminRow> books() {
        return jdbc.query("""
                SELECT d.MaDauSach, d.TenSach, d.NamXuatBan, d.GiaSach, n.TenNXB,
                       GROUP_CONCAT(DISTINCT t.TenTacGia ORDER BY t.TenTacGia SEPARATOR ', ') AS TenTacGia,
                       COUNT(DISTINCT b.MaBanSach) AS SoBan
                FROM DauSach d
                LEFT JOIN NhaXuatBan n ON n.MaNXB = d.MaNXB
                LEFT JOIN TacGia_Sach ts ON ts.MaDauSach = d.MaDauSach
                LEFT JOIN TacGia t ON t.MaTacGia = ts.MaTacGia
                LEFT JOIN BanSach b ON b.MaDauSach = d.MaDauSach
                GROUP BY d.MaDauSach
                ORDER BY d.MaDauSach DESC
                """, (rs, row) -> new BookAdminRow(rs.getString("MaDauSach"), rs.getString("TenSach"),
                rs.getObject("NamXuatBan"), rs.getBigDecimal("GiaSach"), rs.getString("TenNXB"),
                rs.getString("TenTacGia"), rs.getInt("SoBan")));
    }

    public List<BookCopyRow> copies() {
        return jdbc.query("""
                SELECT b.MaBanSach, b.MaDauSach, d.TenSach, b.TinhTrang, b.TinhTrangVatLy
                FROM BanSach b JOIN DauSach d ON d.MaDauSach = b.MaDauSach
                ORDER BY b.MaDauSach DESC, b.MaBanSach
                """, (rs, row) -> new BookCopyRow(rs.getString("MaBanSach"), rs.getString("MaDauSach"),
                rs.getString("TenSach"), rs.getInt("TinhTrang"), rs.getString("TinhTrangVatLy")));
    }

    @Transactional
    public long createBook(String title, int year, BigDecimal price, String genreId,
                           String publisherId, String authorId) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO DauSach (TenSach, NamXuatBan, GiaSach, MaTheLoai, MaNXB)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setInt(2, year);
            statement.setBigDecimal(3, price);
            statement.setString(4, genreId);
            statement.setString(5, publisherId);
            return statement;
        }, key);
        Number generated = key.getKey();
        if (generated == null) throw new IllegalStateException("Không lấy được mã đầu sách mới.");
        long bookId = generated.longValue();
        jdbc.update("INSERT INTO TacGia_Sach (MaDauSach, MaTacGia) VALUES (?, ?)", bookId, authorId);
        return bookId;
    }

    @Transactional
    public int addCopies(String bookId, int quantity) {
        if (quantity < 1 || quantity > 100) {
            throw new IllegalArgumentException("Số bản thêm phải từ 1 đến 100.");
        }
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM DauSach WHERE MaDauSach = ?", Integer.class, bookId);
        if (exists == null || exists == 0) throw new IllegalArgumentException("Không tìm thấy đầu sách.");
        for (int i = 0; i < quantity; i++) {
            String barcode = "MV" + bookId + "-" + UUID.randomUUID().toString().substring(0, 8);
            jdbc.update("""
                    INSERT INTO BanSach (MaBanSach, MaDauSach, TinhTrang, TinhTrangVatLy)
                    VALUES (?, ?, 1, 'Bình thường')
                    """, barcode, bookId);
        }
        return quantity;
    }

    @Transactional
    public CopyRemovalResult removeCopies(List<String> barcodes, String username) {
        int deleted = 0;
        int liquidated = 0;
        int borrowing = 0;
        for (String barcode : barcodes.stream().distinct().toList()) {
            List<Integer> statuses = jdbc.query("SELECT TinhTrang FROM BanSach WHERE MaBanSach = ? FOR UPDATE",
                    (rs, row) -> rs.getInt(1), barcode);
            if (statuses.isEmpty()) continue;
            if (statuses.getFirst() == 0) {
                borrowing++;
                continue;
            }
            Integer history = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ChiTietMuonTra WHERE MaBanSach = ?", Integer.class, barcode);
            if (history != null && history > 0) {
                jdbc.update("UPDATE BanSach SET TinhTrang = 3 WHERE MaBanSach = ?", barcode);
                jdbc.update("""
                        INSERT INTO LichSuHoatDong (NguoiThucHien, HanhDong, ChiTiet)
                        VALUES (?, ?, ?)
                        """, username, "Thanh lý sách " + barcode, "Thanh lý từ quản lý sách");
                liquidated++;
            } else {
                jdbc.update("DELETE FROM BanSach WHERE MaBanSach = ?", barcode);
                deleted++;
            }
        }
        return new CopyRemovalResult(deleted, liquidated, borrowing);
    }

    @Transactional
    public void deleteBook(String id) {
        Integer borrowed = jdbc.queryForObject("SELECT COUNT(*) FROM BanSach WHERE MaDauSach = ? AND TinhTrang = 0",
                Integer.class, id);
        Integer history = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ChiTietMuonTra ct
                JOIN BanSach b ON b.MaBanSach = ct.MaBanSach
                WHERE b.MaDauSach = ?
                """, Integer.class, id);
        if ((borrowed != null && borrowed > 0) || (history != null && history > 0)) {
            throw new IllegalArgumentException("Đầu sách có bản đang mượn hoặc có lịch sử mượn; hãy thanh lý từng bản.");
        }
        jdbc.update("DELETE FROM BanSach WHERE MaDauSach = ?", id);
        jdbc.update("DELETE FROM TacGia_Sach WHERE MaDauSach = ?", id);
        jdbc.update("DELETE FROM DauSach WHERE MaDauSach = ?", id);
    }

    public record BookAdminRow(String id, String title, Object year, BigDecimal price,
                               String publisher, String authors, int copies) {}
    public record BookCopyRow(String barcode, String bookId, String title, int status, String physicalCondition) {}
    public record CopyRemovalResult(int deleted, int liquidated, int borrowing) {}
}
