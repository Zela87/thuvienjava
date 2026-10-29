# Quản Lý Thư Viện - Project Architecture Documentation (Java Version)

## Tổng quan (Project Overview)

Dự án **Quản Lý Thư Viện (Library Management System)** là hệ thống quản lý thư viện tích hợp tính năng: tra cứu, mượn/trả sách, quản lý độc giả/thẻ thư viện, bản sao sách, phiếu mượn, báo cáo sự cố, thanh lý và thống kê.

**Công nghệ chính:**

* **Backend / Core:** Java (OOP, Collections, JDBC / Hibernate/JPA).
* **Database:** MySQL (DB: `QuanLyThuVien`), kết nối qua JDBC Driver.
* **Frontend / UI:** Java Swing (Desktop App) hoặc JavaFX / Spring Boot + Thymeleaf (Web App). (Dưới đây chuẩn hóa theo mô hình ứng dụng Desktop Java Swing hoặc Web Java tiêu chuẩn).
* **Auth:** Role-based (`QuanTriVien`, `ThuThu`, `DocGia`), phân quyền đăng nhập.
* **Key Features:** Mượn/trả sách, kiểm tra hạn mức/quá hạn/thẻ hết hạn, thanh lý/hỏng sách, lịch sử hoạt động, thống kê báo cáo.

---

## Cấu trúc thư mục (Directory Structure - Standard Java Maven Project)

```
thuvienjava/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/library/
│   │   │   │   ├── config/        # Database connection (JDBC connection pool / DBConnection.java)
│   │   │   │   ├── dao/           # Data Access Objects (SQL queries per entity: SachDAO, DocGiaDAO, PhieuMuonDAO...)
│   │   │   │   ├── model/         # Entity classes (DauSach, BanSach, DocGia, PhieuMuon, TacGia...)
│   │   │   │   ├── controller/    # Business logic & event handlers (AuthContronller, MuonTraController...)
│   │   │   │   └── view/          # UI components (JFrame, JPanel, Dialogs: LoginFrame, MainForm, BorrowPanel...)
│   │   │   └── Main.java          # Application entry point
│   │   └── resources/
│   │       ├── css/ / styles/     # UI themes / styling (nếu dùng JavaFX)
│   │       └── images/            # Icons, book covers, avatars
│   └── test/                      # Unit tests (JUnit)
├── pom.xml                        # Maven dependencies (MySQL Connector, FlatLaf UI, etc.)
└── README.md

```

**MVC Pattern trong Java:**

* **Model:** Các lớp đối tượng (`model/`) và DAO kết nối CSDL (`dao/`).
* **View:** Các giao diện màn hình (`view/`).
* **Controller:** Điều phối sự kiện từ View, gọi DAO xử lý dữ liệu và cập nhật lại View (`controller/`).

---

## Cơ sở dữ liệu (Database Schema)

**DB Name:** `QuanLyThuVien` (MySQL).

**Bảng chính & Mối quan hệ:**

* **DauSach** (head books): `MaDauSach` (PK), `TenSach`, `NamXuatBan`, `GiaSach`, `MaTheLoai` → TheLoai, `MaNXB` → NhaXuatBan. **1:N** BanSach, TacGia_Sach.
* **BanSach** (book copies): `MaBanSach` (PK), `MaDauSach` → DauSach, `TinhTrang` (0=borrowed, 1=available, 2=lost, 3=liquidated), `TinhTrangVatLy` (Bình thường/Hỏng/Rách/Mất).
* **DocGia** (readers): `MaDocGia` (PK), `HoTen`, `TenDangNhap` → **1:1** TheThuVien.
* **TheThuVien** (library cards): `SoThe` (PK), `MaDocGia` → DocGia, `SoSachMuonToiDa`, `TrangThai` (0=locked, 1=active), `NgayHetHan`.
* **PhieuMuon** (loan receipts): `MaPhieuMuon` (PK), `SoThe` → TheThuVien, `NgayHenTra`, `NguoiTao`, `GhiChu`. **1:N** ChiTietMuonTra.
* **ChiTietMuonTra** (loan details): `MaPhieuMuon` → PhieuMuon, `MaBanSach` → BanSach, `NgayTraThucTe` (NULL=borrowed).
* **yeucaumuon** (online requests): `MaYeuCau` (PK), `MaDocGia` → DocGia, `MaBanSach` → BanSach, `TrangThai` (ChoDuyet/DaDuyet/TuChoi).
* **TheLoai** (categories): `MaTheLoai` (PK), `TenTheLoai`.
* **TacGia** (authors): `MaTacGia` (PK), `TenTacGia`.
* **TacGia_Sach** (junction): `MaDauSach`, `MaTacGia`.
* **NhaXuatBan** (publishers): `MaNXB` (PK), `TenNXB`.
* **BaoCaoSuCo** (reports), **LichSuHoatDong** (audit log), **TaiKhoan** (users, role: QuanTriVien/ThuThu/DocGia).

---

## Luồng nghiệp vụ chính (Core Logic in Java)

**1. Mượn sách (Borrow Flow - `MuonTraController.java`):**

* **Trực tiếp (3 steps):** Kiểm tra thẻ (hợp lệ, active, chưa hết hạn, không quá hạn, chưa vượt hạn mức) → Kiểm tra sách (`TinhTrang = 1`) → Tạo `PhieuMuon` + `ChiTietMuonTra` dùng Transaction (`conn.setAutoCommit(false)`), `UPDATE BanSach` set `TinhTrang = 0`.
* **Online (DocGia):** Chọn sách có sẵn → `INSERT` bảng `yeucaumuon` (trạng thái `ChoDuyet`) → Thủ thư duyệt → Tự động tạo `PhieuMuon` (hạn 14 ngày).

**2. Trả sách (Return Flow):**

* Giao diện chọn điều kiện trả (Bình thường / Mất / Hỏng / Rách) → Tính tiền phạt (nếu mất = 50% giá sách, quá hạn tính phí ngày trễ) → `UPDATE` bảng `BanSach` (`TinhTrangVatLy`, `TinhTrang = 1/2/3`), cập nhật `NgayTraThucTe` trong `ChiTietMuonTra`.

**3. Quản lý sách (`SachDAO.java`):**

* Thêm Đầu sách + sinh bản sao tự động theo mã (`MVxxx-xxxx`) → Liên kết bảng trung gian `TacGia_Sach` sử dụng `PreparedStatement` chống SQL Injection.

---

## Giao diện & Thành phần (UI Components in Java)

* **Layout chính:** Sử dụng `BorderLayout` hoặc `CardLayout` cho các màn hình Dashboard phân quyền theo Role (`DocGia`: tra cứu/đăng ký mượn; `Admin/ThuThu`: quản lý toàn bộ hệ thống).
* **Components:**
* `JTable` / `DefaultTableModel`: Hiển thị danh sách dữ liệu (Sách, Độc giả, Phiếu mượn) tích hợp tìm kiếm, lọc dữ liệu trực tiếp.
* `JDialog` / `JOptionPane`: Form thêm/sửa, hộp thoại xác nhận xóa, cảnh báo lỗi.
* `PreparedStatement` & `ResultSet`: Đảm bảo an toàn dữ liệu và tối ưu hiệu suất truy vấn.



---

## Hướng dẫn mở rộng / Phát triển tiếp (Scalability Note for Java)

1. **Quản lý kết nối Database (`DBConnection.java`):** Sử dụng `DriverManager` hoặc `HikariCP` connection pool để tối ưu hiệu năng kết nối MySQL.
2. **Xử lý Transaction:** Luôn đóng gói các nghiệp vụ liên quan đến mượn/trả sách trong khối `try-catch` với `connection.commit()` và `connection.rollback()` để đảm bảo tính toàn vẹn dữ liệu.
3. **Quản lý Exception:** Xây dựng custom exception (`LibraryException`) để xử lý các lỗi nghiệp vụ như hết sách, quá hạn thẻ, vượt số lượng mượn tối đa.
