package com.thuvien.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReferenceDataRepository {

    private final JdbcTemplate jdbc;

    public ReferenceDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Genre> genres() {
        return jdbc.query("SELECT MaTheLoai, TenTheLoai FROM TheLoai ORDER BY MaTheLoai DESC",
                (rs, row) -> new Genre(rs.getString("MaTheLoai"), rs.getString("TenTheLoai")));
    }

    public List<Author> authors() {
        return jdbc.query("SELECT MaTacGia, TenTacGia, ChucDanh, NoiLamViec FROM TacGia ORDER BY MaTacGia DESC",
                (rs, row) -> new Author(rs.getString("MaTacGia"), rs.getString("TenTacGia"),
                        rs.getString("ChucDanh"), rs.getString("NoiLamViec")));
    }

    public List<Publisher> publishers() {
        return jdbc.query("SELECT MaNXB, TenNXB, DiaChi FROM NhaXuatBan ORDER BY MaNXB DESC",
                (rs, row) -> new Publisher(rs.getString("MaNXB"), rs.getString("TenNXB"), rs.getString("DiaChi")));
    }

    public void addGenre(String name) {
        jdbc.update("INSERT INTO TheLoai (TenTheLoai) VALUES (?)", name);
    }

    public void deleteGenre(String id) {
        Integer linked = jdbc.queryForObject("SELECT COUNT(*) FROM DauSach WHERE MaTheLoai = ?", Integer.class, id);
        if (linked != null && linked > 0) throw new IllegalArgumentException("Thể loại đang được sách sử dụng.");
        jdbc.update("DELETE FROM TheLoai WHERE MaTheLoai = ?", id);
    }

    public void addAuthor(String name, String title, String workplace) {
        jdbc.update("INSERT INTO TacGia (TenTacGia, ChucDanh, NoiLamViec) VALUES (?, ?, ?)", name, title, workplace);
    }

    public void deleteAuthor(String id) {
        jdbc.update("DELETE FROM TacGia WHERE MaTacGia = ?", id);
    }

    public void savePublisher(String id, String name, String address) {
        if (id == null || id.isBlank()) {
            jdbc.update("INSERT INTO NhaXuatBan (TenNXB, DiaChi) VALUES (?, ?)", name, address);
        } else {
            jdbc.update("UPDATE NhaXuatBan SET TenNXB = ?, DiaChi = ? WHERE MaNXB = ?", name, address, id);
        }
    }

    public void deletePublisher(String id) {
        Integer linked = jdbc.queryForObject("SELECT COUNT(*) FROM DauSach WHERE MaNXB = ?", Integer.class, id);
        if (linked != null && linked > 0) throw new IllegalArgumentException("Nhà xuất bản đang được sách sử dụng.");
        jdbc.update("DELETE FROM NhaXuatBan WHERE MaNXB = ?", id);
    }

    public record Genre(String id, String name) {}
    public record Author(String id, String name, String title, String workplace) {}
    public record Publisher(String id, String name, String address) {}
}
