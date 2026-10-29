package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;

@Repository
public class LibraryRepository {

    private final JdbcTemplate jdbc;

    public LibraryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int countUnreadSupport(String role) {
        if ("QuanTriVien".equals(role)) {
            return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM BaoCaoSuCo
                WHERE TrangThai = 'Chờ xử lý'
                  AND LoaiSuCo IN (?, ?)
                """, Integer.class, "Web lỗi", "Khác");
        }
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM BaoCaoSuCo
                WHERE TrangThai = 'Chờ xử lý'
                  AND LoaiSuCo IN (?, ?, ?)
                """, Integer.class, "Vấn đề về thẻ", "Hết sách", "Khác");
    }

    public int countBooks(String keyword, String criteria) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT d.MaDauSach)
                FROM DauSach d
                LEFT JOIN TheLoai tl ON tl.MaTheLoai = d.MaTheLoai
                LEFT JOIN TacGia_Sach ts ON ts.MaDauSach = d.MaDauSach
                LEFT JOIN TacGia t ON t.MaTacGia = ts.MaTacGia
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        appendSearchFilter(sql, parameters, keyword, criteria);
        return jdbc.queryForObject(sql.toString(), Integer.class, parameters.toArray());
    }

    public List<BookSummary> searchBooks(String keyword, String criteria, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT d.MaDauSach, d.TenSach, d.NamXuatBan, d.HinhAnh,
                       tl.TenTheLoai,
                       GROUP_CONCAT(DISTINCT t.TenTacGia ORDER BY t.TenTacGia SEPARATOR ', ') AS TacGiaLienKet,
                       (SELECT COUNT(*) FROM BanSach b
                        WHERE b.MaDauSach = d.MaDauSach AND b.TinhTrang = 1) AS SachCon
                FROM DauSach d
                LEFT JOIN TheLoai tl ON tl.MaTheLoai = d.MaTheLoai
                LEFT JOIN TacGia_Sach ts ON ts.MaDauSach = d.MaDauSach
                LEFT JOIN TacGia t ON t.MaTacGia = ts.MaTacGia
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        appendSearchFilter(sql, parameters, keyword, criteria);
        sql.append(" GROUP BY d.MaDauSach ORDER BY d.MaDauSach DESC LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);
        RowMapper<BookSummary> mapper = (rs, rowNum) -> new BookSummary(
                rs.getString("MaDauSach"), rs.getString("TenSach"),
                rs.getObject("NamXuatBan"), rs.getString("HinhAnh"),
                rs.getString("TenTheLoai"), rs.getString("TacGiaLienKet"), rs.getInt("SachCon"));
        return jdbc.query(sql.toString(), mapper, parameters.toArray());
    }

    private void appendSearchFilter(StringBuilder sql, List<Object> parameters, String keyword, String criteria) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) return;
        String filterColumn = switch (criteria == null ? "TenSach" : criteria) {
            case "TacGia" -> "t.TenTacGia";
            case "TenTheLoai" -> "tl.TenTheLoai";
            default -> "d.TenSach";
        };
        sql.append(" AND ").append(filterColumn).append(" LIKE ?");
        parameters.add("%" + normalized + "%");
    }

    public record BookSummary(String id, String title, Object publicationYear, String image,
                              String category, String authors, int availableCopies) {}
}
