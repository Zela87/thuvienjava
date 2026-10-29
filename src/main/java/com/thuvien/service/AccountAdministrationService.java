package com.thuvien.service;

import com.thuvien.repo.AccountAdminRepository;
import com.thuvien.repo.AccountAdminRepository.AccountRow;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountAdministrationService {

    private static final List<String> ROLES = List.of("QuanTriVien", "ThuThu", "DocGia");
    private final AccountAdminRepository accounts;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AccountAdministrationService(AccountAdminRepository accounts, JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.jdbc = jdbc;
    }

    public List<AccountRow> accounts() { return accounts.accounts(); }

    @Transactional
    public void create(String username, String password, String name, String role) {
        if (username == null || username.trim().length() < 3) throw new IllegalArgumentException("Tên đăng nhập cần ít nhất 3 ký tự.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự.");
        if (!ROLES.contains(role)) throw new IllegalArgumentException("Vai trò không hợp lệ.");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cần nhập họ và tên để tạo hồ sơ người dùng.");
        }
        Integer roleId = jdbc.queryForObject("SELECT MaNhom FROM NhomNguoiDung WHERE TenNhom = ?",
                Integer.class, role);
        if (roleId == null) throw new IllegalArgumentException("Không tìm thấy nhóm người dùng.");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE TenDangNhap = ?", Integer.class, username.trim());
        if (count != null && count > 0) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        try {
            accounts.create(username.trim(), encoder.encode(password), name == null ? "" : name.trim(), roleId, role);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại hoặc hồ sơ không hợp lệ.");
        }
    }

    public void toggleStatus(String username, int currentStatus, String actor) {
        if (username.equals(actor)) throw new IllegalArgumentException("Không thể khóa tài khoản đang đăng nhập.");
        if (currentStatus != 0 && currentStatus != 1) throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        accounts.setStatus(username, currentStatus == 1 ? 0 : 1);
    }
}
