package com.thuvien.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.sql.Statement;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@Service
public class OnlineLoanService {

    private final JdbcTemplate jdbc;

    public OnlineLoanService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ReaderLoanSummary readerSummary(String username) {
        ReaderCard card = loadCard(username, false);
        if (card == null) return new ReaderLoanSummary(null, null, 0, 0, 0, 0, "Không tìm thấy hồ sơ độc giả.");
        int borrowed = borrowedCount(card.cardNumber());
        int pending = pendingCount(card.readerId());
        int overdue = overdueCount(card.cardNumber());
        String error = eligibilityMessage(card, overdue);
        int remaining = Math.max(0, card.limit() - borrowed - pending);
        return new ReaderLoanSummary(card.readerId(), card.cardNumber(), borrowed, pending, card.limit(), remaining, error);
    }

    public List<AvailableCopy> availableCopies() {
        return jdbc.query("""
                SELECT b.MaBanSach, d.TenSach, tl.TenTheLoai,
                       GROUP_CONCAT(DISTINCT t.TenTacGia ORDER BY t.TenTacGia SEPARATOR ', ') AS TacGia
                FROM BanSach b
                JOIN DauSach d ON d.MaDauSach = b.MaDauSach
                LEFT JOIN TheLoai tl ON tl.MaTheLoai = d.MaTheLoai
                LEFT JOIN TacGia_Sach ts ON ts.MaDauSach = d.MaDauSach
                LEFT JOIN TacGia t ON t.MaTacGia = ts.MaTacGia
                WHERE b.TinhTrang = 1
                  AND NOT EXISTS (SELECT 1 FROM yeucaumuon yc
                                  WHERE yc.MaBanSach = b.MaBanSach AND yc.TrangThai = 'ChoDuyet')
                GROUP BY b.MaBanSach, d.TenSach, tl.TenTheLoai
                ORDER BY d.TenSach, b.MaBanSach
                """, (rs, row) -> new AvailableCopy(rs.getString("MaBanSach"), rs.getString("TenSach"),
                rs.getString("TacGia"), rs.getString("TenTheLoai")));
    }

    public List<RequestRow> requests(String username) {
        String readerId = readerId(username);
        return jdbc.query("""
                SELECT yc.MaYeuCau, yc.MaBanSach, d.TenSach, yc.TrangThai
                FROM yeucaumuon yc
                JOIN BanSach b ON b.MaBanSach = yc.MaBanSach
                JOIN DauSach d ON d.MaDauSach = b.MaDauSach
                WHERE yc.MaDocGia = ?
                ORDER BY yc.MaYeuCau DESC
                """, (rs, row) -> new RequestRow(rs.getString("MaYeuCau"), rs.getString("MaBanSach"),
                rs.getString("TenSach"), rs.getString("TrangThai")), readerId);
    }

    public List<PendingRequest> pendingRequests() {
        return jdbc.query("""
                SELECT yc.MaYeuCau, yc.MaDocGia, yc.MaBanSach, doc.HoTen, ttv.SoThe,
                       d.TenSach, yc.TrangThai
                FROM yeucaumuon yc
                JOIN DocGia doc ON doc.MaDocGia = yc.MaDocGia
                LEFT JOIN TheThuVien ttv ON ttv.MaDocGia = doc.MaDocGia
                JOIN BanSach b ON b.MaBanSach = yc.MaBanSach
                JOIN DauSach d ON d.MaDauSach = b.MaDauSach
                WHERE yc.TrangThai = 'ChoDuyet'
                ORDER BY yc.MaYeuCau ASC
                """, (rs, row) -> new PendingRequest(rs.getString("MaYeuCau"),
                rs.getString("MaDocGia"), rs.getString("MaBanSach"), rs.getString("HoTen"),
                rs.getString("SoThe"), rs.getString("TenSach")));
    }

    @Transactional
    public long approveRequest(String requestId, String staffUsername) {
        List<PendingRequest> requestRows = jdbc.query("""
                SELECT yc.MaYeuCau, yc.MaDocGia, yc.MaBanSach, doc.HoTen, ttv.SoThe,
                       d.TenSach
                FROM yeucaumuon yc
                JOIN DocGia doc ON doc.MaDocGia = yc.MaDocGia
                LEFT JOIN TheThuVien ttv ON ttv.MaDocGia = doc.MaDocGia
                JOIN BanSach b ON b.MaBanSach = yc.MaBanSach
                JOIN DauSach d ON d.MaDauSach = b.MaDauSach
                WHERE yc.MaYeuCau = ? AND yc.TrangThai = 'ChoDuyet'
                FOR UPDATE
                """, (rs, row) -> new PendingRequest(rs.getString("MaYeuCau"),
                rs.getString("MaDocGia"), rs.getString("MaBanSach"), rs.getString("HoTen"),
                rs.getString("SoThe"), rs.getString("TenSach")), requestId);
        if (requestRows.isEmpty()) throw new LoanRuleException("Yêu cầu không còn ở trạng thái chờ duyệt.");
        PendingRequest request = requestRows.get(0);
        ReaderCard card = loadCardById(request.readerId());
        if (card == null || card.cardNumber() == null) throw new LoanRuleException("Độc giả chưa có thẻ.");
        int overdue = overdueCount(card.cardNumber());
        String eligibility = eligibilityMessage(card, overdue);
        if (eligibility != null) throw new LoanRuleException(eligibility);
        if (borrowedCount(card.cardNumber()) >= card.limit()) throw new LoanRuleException("Độc giả đã đạt giới hạn mượn.");

        List<Integer> copyStatuses = jdbc.query("SELECT TinhTrang FROM BanSach WHERE MaBanSach = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), request.copyId());
        if (copyStatuses.isEmpty() || copyStatuses.get(0) != 1) {
            throw new LoanRuleException("Bản sách không còn khả dụng.");
        }
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO PhieuMuon (SoThe, NgayHenTra, NguoiTao, GhiChu)
                    VALUES (?, DATE_ADD(CURDATE(), INTERVAL 14 DAY), ?, 'Từ đăng ký online')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, card.cardNumber());
            statement.setString(2, staffUsername);
            return statement;
        }, key);
        Number loanId = key.getKey();
        if (loanId == null) throw new IllegalStateException("Không tạo được phiếu mượn.");
        jdbc.update("INSERT INTO ChiTietMuonTra (MaPhieuMuon, MaBanSach) VALUES (?, ?)", loanId.longValue(), request.copyId());
        int updated = jdbc.update("UPDATE BanSach SET TinhTrang = 0 WHERE MaBanSach = ? AND TinhTrang = 1", request.copyId());
        if (updated != 1) throw new LoanRuleException("Bản sách vừa được mượn bởi yêu cầu khác.");
        int requestUpdated = jdbc.update("UPDATE yeucaumuon SET TrangThai = 'DaDuyet' WHERE MaYeuCau = ? AND TrangThai = 'ChoDuyet'", requestId);
        if (requestUpdated != 1) throw new LoanRuleException("Yêu cầu đã được xử lý trước đó.");
        return loanId.longValue();
    }

    @Transactional
    public boolean rejectRequest(String requestId) {
        return jdbc.update("UPDATE yeucaumuon SET TrangThai = 'TuChoi' WHERE MaYeuCau = ? AND TrangThai = 'ChoDuyet'",
                requestId) == 1;
    }

    @Transactional
    public long issueDirectLoan(String cardNumber, String copyId, LocalDate dueDate,
                                String note, String staffUsername) {
        List<ReaderCard> cardRows = jdbc.query("""
                SELECT d.MaDocGia, t.SoThe, t.SoSachMuonToiDa, t.TrangThai, t.NgayHetHan
                FROM TheThuVien t JOIN DocGia d ON d.MaDocGia = t.MaDocGia
                WHERE t.SoThe = ? FOR UPDATE
                """, (rs, row) -> new ReaderCard(rs.getString("MaDocGia"), rs.getString("SoThe"),
                rs.getInt("SoSachMuonToiDa"), rs.getInt("TrangThai"),
                rs.getDate("NgayHetHan") == null ? null : rs.getDate("NgayHetHan").toLocalDate()), cardNumber);
        if (cardRows.isEmpty()) throw new LoanRuleException("Không tìm thấy thẻ thư viện.");
        ReaderCard card = cardRows.get(0);
        int overdue = overdueCount(card.cardNumber());
        String eligibility = eligibilityMessage(card, overdue);
        if (eligibility != null) throw new LoanRuleException(eligibility);
        if (borrowedCount(card.cardNumber()) >= card.limit()) throw new LoanRuleException("Độc giả đã đạt giới hạn mượn.");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (dueDate == null || dueDate.isBefore(today)) throw new LoanRuleException("Hạn trả phải từ hôm nay trở đi.");

        List<Integer> copyStatuses = jdbc.query("SELECT TinhTrang FROM BanSach WHERE MaBanSach = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), copyId);
        if (copyStatuses.isEmpty() || copyStatuses.get(0) != 1) throw new LoanRuleException("Bản sách không còn khả dụng.");
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO PhieuMuon (SoThe, NgayHenTra, NguoiTao, GhiChu)
                    VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, card.cardNumber());
            statement.setDate(2, java.sql.Date.valueOf(dueDate));
            statement.setString(3, staffUsername);
            statement.setString(4, note == null ? "" : note.trim());
            return statement;
        }, key);
        Number loanId = key.getKey();
        if (loanId == null) throw new IllegalStateException("Không tạo được phiếu mượn.");
        jdbc.update("INSERT INTO ChiTietMuonTra (MaPhieuMuon, MaBanSach) VALUES (?, ?)", loanId.longValue(), copyId);
        int changed = jdbc.update("UPDATE BanSach SET TinhTrang = 0 WHERE MaBanSach = ? AND TinhTrang = 1", copyId);
        if (changed != 1) throw new LoanRuleException("Bản sách vừa được mượn bởi giao dịch khác.");
        return loanId.longValue();
    }

    @Transactional
    public void requestCopies(String username, List<String> copyIds) {
        if (copyIds == null || copyIds.isEmpty()) throw new LoanRuleException("Hãy chọn ít nhất một bản sách.");
        ReaderCard card = loadCard(username, true);
        if (card == null) throw new LoanRuleException("Không tìm thấy hồ sơ độc giả.");
        int overdue = overdueCount(card.cardNumber());
        String error = eligibilityMessage(card, overdue);
        if (error != null) throw new LoanRuleException(error);
        List<String> uniqueIds = copyIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        int capacity = card.limit() - borrowedCount(card.cardNumber()) - pendingCount(card.readerId());
        if (uniqueIds.isEmpty()) throw new LoanRuleException("Hãy chọn ít nhất một bản sách.");
        if (uniqueIds.size() > capacity) throw new LoanRuleException("Số yêu cầu vượt quá số sách còn được mượn.");

        for (String copyId : uniqueIds) {
            List<Integer> statuses = jdbc.query("SELECT TinhTrang FROM BanSach WHERE MaBanSach = ? FOR UPDATE",
                    (rs, row) -> rs.getInt(1), copyId);
            if (statuses.isEmpty() || statuses.get(0) != 1) {
                throw new LoanRuleException("Một trong các bản sách đã hết khả dụng: " + copyId);
            }
            Integer pendingElsewhere = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM yeucaumuon
                    WHERE MaBanSach = ? AND TrangThai = 'ChoDuyet'
                    """, Integer.class, copyId);
            if (pendingElsewhere != null && pendingElsewhere > 0) {
                throw new LoanRuleException("Bản sách đã có yêu cầu đang chờ: " + copyId);
            }
            jdbc.update("INSERT INTO yeucaumuon (MaDocGia, MaBanSach, TrangThai) VALUES (?, ?, 'ChoDuyet')",
                    card.readerId(), copyId);
        }
    }

    @Transactional
    public boolean cancelRequest(String username, String requestId) {
        String readerId = readerId(username);
        return jdbc.update("""
                DELETE FROM yeucaumuon
                WHERE MaYeuCau = ? AND MaDocGia = ? AND TrangThai = 'ChoDuyet'
                """, requestId, readerId) > 0;
    }

    private ReaderCard loadCard(String username, boolean lock) {
        String sql = """
                SELECT d.MaDocGia, t.SoThe, t.SoSachMuonToiDa, t.TrangThai, t.NgayHetHan
                FROM DocGia d LEFT JOIN TheThuVien t ON t.MaDocGia = d.MaDocGia
                WHERE d.TenDangNhap = ?
                """ + (lock ? " FOR UPDATE" : "");
        List<ReaderCard> result = jdbc.query(sql, (rs, row) -> new ReaderCard(
                rs.getString("MaDocGia"), rs.getString("SoThe"),
                rs.getInt("SoSachMuonToiDa"), rs.getObject("TrangThai") == null ? null : rs.getInt("TrangThai"),
                rs.getDate("NgayHetHan") == null ? null : rs.getDate("NgayHetHan").toLocalDate()), username);
        return result.isEmpty() ? null : result.get(0);
    }

    private String readerId(String username) {
        return jdbc.queryForObject("SELECT MaDocGia FROM DocGia WHERE TenDangNhap = ?", String.class, username);
    }

    private int borrowedCount(String cardNumber) {
        if (cardNumber == null) return 0;
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM PhieuMuon pm
                JOIN ChiTietMuonTra ct ON ct.MaPhieuMuon = pm.MaPhieuMuon
                WHERE pm.SoThe = ? AND ct.NgayTraThucTe IS NULL
                """, Integer.class, cardNumber);
        return count == null ? 0 : count;
    }

    private int pendingCount(String readerId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM yeucaumuon WHERE MaDocGia = ? AND TrangThai = 'ChoDuyet'",
                Integer.class, readerId);
        return count == null ? 0 : count;
    }

    private int overdueCount(String cardNumber) {
        if (cardNumber == null) return 0;
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM PhieuMuon pm
                JOIN ChiTietMuonTra ct ON ct.MaPhieuMuon = pm.MaPhieuMuon
                WHERE pm.SoThe = ? AND ct.NgayTraThucTe IS NULL AND pm.NgayHenTra < CURDATE()
                """, Integer.class, cardNumber);
        return count == null ? 0 : count;
    }

    private ReaderCard loadCardById(String readerId) {
        List<ReaderCard> result = jdbc.query("""
                SELECT d.MaDocGia, t.SoThe, t.SoSachMuonToiDa, t.TrangThai, t.NgayHetHan
                FROM DocGia d LEFT JOIN TheThuVien t ON t.MaDocGia = d.MaDocGia
                WHERE d.MaDocGia = ? FOR UPDATE
                """, (rs, row) -> new ReaderCard(rs.getString("MaDocGia"), rs.getString("SoThe"),
                rs.getInt("SoSachMuonToiDa"), rs.getObject("TrangThai") == null ? null : rs.getInt("TrangThai"),
                rs.getDate("NgayHetHan") == null ? null : rs.getDate("NgayHetHan").toLocalDate()), readerId);
        return result.isEmpty() ? null : result.get(0);
    }

    private String eligibilityMessage(ReaderCard card, int overdue) {
        if (card.cardNumber() == null) return "Bạn chưa có thẻ thư viện.";
        if (card.status() == null || card.status() == 0) return "Thẻ của bạn đang bị khóa.";
        if (card.expiresAt() == null || card.expiresAt().isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) return "Thẻ của bạn đã hết hạn.";
        if (overdue > 0) return "Bạn đang có sách quá hạn.";
        return null;
    }

    private record ReaderCard(String readerId, String cardNumber, int limit, Integer status, LocalDate expiresAt) {}
    public record ReaderLoanSummary(String readerId, String cardNumber, int borrowed, int pending,
                                    int limit, int remaining, String error) {}
    public record AvailableCopy(String barcode, String title, String authors, String category) {}
    public record RequestRow(String requestId, String barcode, String title, String status) {}
    public record PendingRequest(String requestId, String readerId, String copyId, String readerName,
                                 String cardNumber, String title) {}
    public static class LoanRuleException extends RuntimeException {
        public LoanRuleException(String message) { super(message); }
    }
}
