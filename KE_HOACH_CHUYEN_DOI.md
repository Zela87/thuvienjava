# Kế hoạch chuyển dự án PHP sang Java Spring Boot

Tài liệu này theo dõi quá trình chuyển hệ thống quản lý thư viện từ `project` (PHP) sang `thuvienjava` (Java 21, Spring Boot, Thymeleaf, MySQL). Mỗi phần chỉ đánh dấu hoàn thành khi mã Java đã chạy được và luồng nghiệp vụ tương ứng đã được đối chiếu với PHP.

## Cách theo dõi

- `[ ]` Chưa bắt đầu · `[-]` Đang làm · `[x]` Hoàn thành · `[!]` Bị chặn/cần quyết định.
- Cập nhật trạng thái, ngày, người phụ trách và ghi chú ngay tại mục tương ứng.
- Mỗi module hoàn thành cần ghi đường dẫn Java thay thế, cách kiểm tra thủ công và mọi khác biệt nghiệp vụ còn lại.
- PHP là bản đối chiếu trong thời gian chuyển đổi; không xóa hoặc sửa cấu trúc PHP để thay thế Java.

## Phạm vi cần chuyển

- Xác thực và tài khoản: đăng nhập, đăng xuất, đăng ký, phân quyền `QuanTriVien`, `ThuThu`, `DocGia`, hồ sơ, đổi mật khẩu, phiên đăng nhập.
- Danh mục: tra cứu sách; đầu sách, bản sách, thể loại, tác giả, nhà xuất bản.
- Bạn đọc: hồ sơ độc giả, thẻ thư viện, trạng thái/hạn thẻ và cấp thẻ.
- Mượn trả: mượn trực tiếp, yêu cầu mượn online, duyệt/từ chối, trả sách, tình trạng vật lý và tiền phạt.
- Quản trị và hỗ trợ: người dùng, thanh lý, báo cáo sự cố/phản hồi, lịch sử hoạt động, thống kê.
- Giao diện chung: trang chủ, tìm kiếm, điều hướng theo vai trò, thông báo, CSS/JS và ảnh/tài nguyên.

## Các giai đoạn

### 0. Khảo sát và chốt quy tắc nghiệp vụ — `[-]` (MySQL hoạt động; còn kiểm thử từng nghiệp vụ)

- [x] Liệt kê màn hình PHP trong `views/` và xử lý trong `actions/`.
- [-] Đã kết nối DB `QuanLyThuVien`, kiểm tra 16 bảng/cột chính bằng `information_schema`, số dòng các bảng chính và chạy truy vấn catalog Java. Chưa rà từng truy vấn ghi dữ liệu/khóa ngoại bằng luồng có đăng nhập.
- [x] Ghi lại ma trận quyền cho các route Java bên dưới; cần kiểm tra cả GET/POST bằng tài khoản từng vai trò.
- [-] Đã đối chiếu mã PHP và schema: hạn online 14 ngày, hạn mức theo `TheThuVien.SoSachMuonToiDa` (DB hiện mặc định 3), tiền phạt theo loại hư hỏng/mất và quá hạn; cần kiểm thử từng trường hợp trên dữ liệu thử.
- [x] Ghi lại các lỗi/lệch đã phát hiện: PHP đăng ký tự tạo `DG+time` trong khi DB thật khai báo `DocGia.MaDocGia` là số tự tăng; luồng mượn trực tiếp/xin mượn thiếu transaction và khóa bản sách khi ghi; một số thao tác thẻ báo thành công mà không kiểm tra số dòng cập nhật; mật khẩu PHP lưu plaintext.

**Đầu ra:** bảng route PHP → route Java, ma trận vai trò, sơ đồ quan hệ DB và danh sách quy tắc nghiệp vụ đã xác nhận.

| Vai trò | Route Java | Quyền |
|---|---|---|
| Khách | `/`, `/index`, `/tra-cuu`, `/login`, `/register` | Tra cứu và xác thực |
| Độc giả | `/dang-ky-muon`, `/thong-tin`, `/doi-mat-khau`, `/bao-cao` | Yêu cầu mượn, hồ sơ, đổi mật khẩu, gửi báo cáo |
| Thủ thư | `/sach`, `/the-loai`, `/tac-gia`, `/nxb`, `/doc-gia`, `/the-thu-vien`, `/muon-tra`, `/phieu-muon`, `/thanh-ly`, `/lich-su`, `/thong-ke`, `/bao-cao` | Quản lý nghiệp vụ thư viện và phản hồi báo cáo theo loại |
| Quản trị viên | Các route thủ thư cùng `/nguoi-dung` | Quản trị tài khoản và toàn bộ nghiệp vụ |
| Người dùng đăng nhập | `/logout` | Kết thúc phiên |

| Chức năng PHP | Route Java | Controller/service Java |
|---|---|---|
| `Index.php`, `tra-cuu-sach.php` | `/`, `/tra-cuu` | `CatalogController`, `LibraryRepository` |
| `Login.php`, `Register.php` | `/login`, `/register` | `AuthController`, `AccountService` |
| Hồ sơ, đổi mật khẩu | `/thong-tin`, `/doi-mat-khau` | `ProfileController`, `ProfileRepository` |
| `quan-ly-nguoi-dung.php` | `/nguoi-dung` | `AccountAdminController`, `AccountAdministrationService` |
| Quản lý sách/danh mục/NXB/tác giả/thể loại | `/sach`, `/nxb`, `/tac-gia`, `/the-loai` | `BookManagementController`, `ReferenceDataController` |
| `quan-ly-doc-gia.php` | `/doc-gia`, `/the-thu-vien` | `ReaderController`, `ReaderRepository` |
| `dang-ky-muon.php` | `/dang-ky-muon` | `OnlineLoanController`, `OnlineLoanService` |
| `muon-tra-sach.php`, duyệt yêu cầu | `/muon-tra` | `StaffLoanController`, `OnlineLoanService` |
| `quan-ly-phieu-muon.php` | `/phieu-muon` | `LoanManagementController`, `LoanManagementService` |
| `thanh-ly-sach.php` | `/thanh-ly` | `LiquidationController`, `LiquidationService` |
| `bao-cao-su-co.php` | `/bao-cao` | `SupportReportController`, `SupportReportRepository` |
| `lich-su-he-thong.php` | `/lich-su` | `ActivityController`, `ActivityRepository` |
| `thong-ke.php` | `/thong-ke` | `StatisticsController`, `StatisticsRepository` |

Quan hệ chính đã thấy trong DB: `NhomNguoiDung → TaiKhoan → (DocGia | NhanVien)`; `DocGia → TheThuVien → PhieuMuon → ChiTietMuonTra → BanSach → DauSach`; `DauSach → TheLoai/NhaXuatBan` và liên kết nhiều-nhiều với `TacGia` qua `TacGia_Sach`; `DocGia/BanSach → yeucaumuon`. Cần kiểm tra thêm các khóa ngoại thực khai báo và cascade trước khi thay đổi schema.

### 1. Nền tảng dự án Java — `[-]`

- [x] Kiểm tra phiên bản Spring Boot/dependencies; Maven biên dịch `release 21` bằng JDK 22.
- [x] Chuẩn hóa cấu hình môi trường: URL DB, thông tin đăng nhập, cổng (`SERVER_PORT`), session; mật khẩu lấy từ biến môi trường.
- [x] Tạo cấu trúc ban đầu `repo` và `service` bên cạnh package `web` hiện có.
- [-] Cấu hình `JdbcTemplate`/transaction; kết nối thật và catalog đã xác nhận. Còn chạy các thao tác ghi với vai trò thật.
- [x] Không thêm migration tự động vì đang dùng schema PHP có dữ liệu thật; tránh để ứng dụng tự sửa DB hiện hữu. Thay đổi schema nếu cần phải có quyết định riêng.
- [-] Trang gốc/đăng nhập và CSS nền hoạt động; các template nghiệp vụ đã chuyển, còn đối chiếu giao diện toàn bộ.
- [x] Ứng dụng đã chạy; trang chủ/đăng nhập/truy vấn catalog trả HTTP 200.

**Điều kiện hoàn tất:** ứng dụng build/chạy độc lập; cấu hình nhạy cảm không nằm trong mã nguồn; kết nối DB được xác nhận.

### 2. Đăng nhập, tài khoản và phân quyền — `[-]`

- [-] Đăng nhập/đăng xuất session (`auth-Login.php`, `auth-Index.php`); đã thêm form/controller và kiểm tra legacy plaintext hoặc BCrypt. Schema xác nhận `MatKhau` là `varchar(255)`, đủ lưu BCrypt; chưa thử với thông tin đăng nhập thật.
- [-] Đăng ký độc giả (`auth-Register.php`); đã thêm form, BCrypt, transaction và dùng mã `MaDocGia` tự tăng đúng schema thật. Chưa gửi đăng ký thử để tránh thêm dữ liệu vào DB thật.
- [-] Tạo/khóa/mở tài khoản (`auth-quan-ly-nguoi-dung.php`); vai trò được ánh xạ qua `NhomNguoiDung`, hồ sơ độc giả/nhân viên được tạo cùng transaction. Chưa gửi thao tác ghi thử lên DB thật.
- [-] Xem/cập nhật hồ sơ, đổi mật khẩu (`thong-tin-ca-nhan.php`, `doi-mat-khau.php`); mật khẩu cũ được kiểm tra theo dạng legacy hoặc BCrypt, mật khẩu mới dùng BCrypt. Chưa xác nhận DB.
- [x] Áp dụng kiểm tra quyền phía server bằng `AuthInterceptor`, rà soát route GET/POST và kiểm tra vai trò tại thao tác gửi/phản hồi báo cáo; context path được tính trong redirect. Chưa thử giả lập từng vai trò.
- [ ] Chuẩn hóa lưu/xác minh mật khẩu an toàn; xử lý dữ liệu mật khẩu PHP cũ theo phương án tương thích được duyệt.

**Điều kiện hoàn tất:** mỗi vai trò chỉ truy cập được chức năng của mình; đăng nhập sai/đúng, đăng xuất, hết phiên và đổi mật khẩu được kiểm tra.

### 3. Tra cứu và danh mục sách — `[-]`

- [-] Trang chủ và tìm kiếm theo tên sách/tác giả/thể loại (`Index.php`, `tra-cuu-sach.php`); đã chuyển truy vấn sang `CatalogController`/`LibraryRepository`, xác nhận 10/10 đầu sách từ DB và thêm phân trang 12 đầu sách/trang. Cần thử với dữ liệu trên 12 đầu sách và rà soát hiển thị ảnh.
- [-] CRUD thể loại (`quan-ly-the-loai.php`); đã thêm danh sách, thêm và chặn xóa khi đang được đầu sách dùng.
- [-] CRUD tác giả (`quan-ly-tac-gia.php`); đã thêm danh sách, thêm và chặn xóa qua ràng buộc liên kết.
- [-] CRUD nhà xuất bản (`quan-ly-nha-xuat-ban.php`); đã thêm danh sách, thêm/sửa và chặn xóa khi đang được đầu sách dùng.
- [-] CRUD đầu sách, liên kết tác giả và tạo/quản lý bản sách (`quan-ly-sach.php`); đã thêm đầu sách+tác giả theo transaction, thêm tối đa 100 bản/lần, xóa bản chưa mượn và thanh lý bản có lịch sử. Xóa đầu sách bị chặn nếu có lịch sử hoặc bản đang mượn.
- [-] Đã thêm phân trang tra cứu (12 đầu sách/trang); cần kiểm thử các trang tiếp theo và các thao tác CRUD bằng phiên đăng nhập.

**Điều kiện hoàn tất:** tìm kiếm đúng; tạo/sửa/xóa theo quyền; liên kết và số lượng bản sách khớp DB.

### 4. Độc giả và thẻ thư viện — `[-]`

- [-] Danh sách và tìm kiếm độc giả (`quan-ly-doc-gia.php`); đã thêm hiển thị hồ sơ và thông tin thẻ.
- [-] Cấp thẻ một năm, gia hạn, khóa/mở thẻ và tiền đặt cọc đã được chuyển; chưa xác nhận với DB thật.
- [ ] Kiểm tra dữ liệu độc giả/thẻ trước khi cho phép đăng ký hoặc mượn.

**Điều kiện hoàn tất:** trạng thái thẻ và hiệu lực ngày tháng nhất quán giữa trang độc giả và nghiệp vụ mượn.

### 5. Mượn trực tiếp, đăng ký online và trả sách — `[-]`

- [-] Mượn trực tiếp: đã thêm kiểm tra thẻ, hết hạn, khóa, sách quá hạn, hạn mức và tình trạng bản sách; tạo phiếu/chi tiết/cập nhật bản trong transaction (`auth-muon-tra.php`). Chưa chạy với DB.
- [-] Đăng ký/hủy yêu cầu mượn online (`auth-dang-ky-muon.php`); đã kiểm tra thẻ, hạn mức, sách quá hạn và khóa bản sách để ngăn yêu cầu trùng.
- [-] Duyệt/từ chối yêu cầu; đã tạo phiếu hạn 14 ngày và cập nhật bản sách/yêu cầu trong transaction. Chưa kiểm tra trên DB thật.
- [-] Trả sách; đã ghi ngày trả, tình trạng thực tế, lịch sử bản sách và tính khoản phạt theo PHP: mất 100%, hỏng 50%, rách 20%, quá hạn 10% giá sách/ngày. Các cập nhật và nhật ký chạy trong transaction; chưa xác nhận DB.
- [x] Các thao tác nhiều bước dùng transaction, khóa bản sách `FOR UPDATE` và cập nhật có điều kiện để chặn mượn trùng.
- [-] Danh sách phiếu đang mở có tìm kiếm/lọc; lịch sử phiếu đã trả cũng đã có trong `ActivityRepository`/`activity-history.html`, còn cần xác minh kết quả trên DB bằng phiên thủ thư.

**Điều kiện hoàn tất:** kiểm tra các luồng thành công và bị từ chối; số bản sách, chi tiết mượn, phiếu mượn luôn đồng bộ.

### 6. Thanh lý, báo cáo sự cố, nhật ký và thống kê — `[-]`

- [-] Thanh lý bản sách có sẵn/hỏng, chặn bản đang mượn và ghi lý do/nhật ký (`auth-thanh-ly-sach.php`); chưa chạy với DB.
- [-] Bạn đọc gửi báo cáo; nhân viên theo vai trò xem và phản hồi/cập nhật trạng thái (`auth-bao-cao-su-co.php`); chưa chạy với DB.
- [-] Nhật ký hoạt động (500 gần nhất), lịch sử mượn (50 gần nhất) và danh sách thanh lý (`auth-lich-su-he-thong.php`) đã thêm.
- [-] Thống kê đầu sách, độc giả, đang mượn, tiền phạt, bản thanh lý và danh sách quá hạn đã thêm (`thong-ke.php`); thông báo trên layout/route cần đối chiếu DB.

**Điều kiện hoàn tất:** dữ liệu truy vấn khớp DB; thao tác thay đổi trạng thái có người thực hiện và thời gian.

### 7. Giao diện, dữ liệu và đối chiếu hoàn tất — `[ ]`

- [-] Topbar dùng chung/menu theo vai trò đã thêm; rà soát route xong, trang chủ/đăng nhập đã trả HTTP 200; kiểm tra giao diện và quyền từng vai trò còn lại.
- [-] Đã xác nhận toàn bộ 19 CSS, 3 JS và 5 ảnh trong `static/` khớp với tài nguyên Java hiện có; trang mới dùng `app.css` và menu responsive. Gắn từng stylesheet cũ vào màn hình tương ứng và rà soát responsive còn lại.
- [ ] Đối chiếu dữ liệu thật trên bản Java; xác nhận không nhân đôi hoặc mất dữ liệu.
- [ ] Đi hết checklist vai trò: khách, độc giả, thủ thư, quản trị viên.
- [ ] So khớp route/chức năng PHP với Java; ghi rõ phần bỏ, đổi hoặc chưa chuyển.
- [ ] Chốt cách chạy Java và thời điểm chuyển người dùng khỏi bản PHP.

**Điều kiện hoàn tất:** checklist nghiệp vụ và quyền truy cập được duyệt; bản Java có thể vận hành độc lập theo cách triển khai đã thống nhất.

## Bảng theo dõi module

| Module | Trạng thái | Phụ thuộc | PHP đối chiếu | Java hoàn tất |
|---|---|---|---|---|
| Khảo sát DB và quy tắc | Đang làm | — | `config/db.php`, `actions/*` | DB và cấu trúc bảng chính đã kiểm tra; cần đối chiếu nghiệp vụ |
| Nền tảng Spring Boot | Đang làm | Khảo sát DB | — | Maven Test thành công; app và trang catalog hoạt động |
| Xác thực và tài khoản | Đang làm | Nền tảng | `auth-Login.php`, `auth-Register.php`, `auth-quan-ly-nguoi-dung.php` | Đã sửa ánh xạ MaNhom và MaDocGia theo schema thật; chưa thử thao tác ghi/đăng nhập |
| Hồ sơ cá nhân | Đang làm | Xác thực | `auth-doi-mat-khau.php`, trang thông tin cá nhân | Cập nhật hồ sơ/đổi mật khẩu đã viết; cần đối chiếu bảng `NhanVien` và DB |
| Tra cứu sách | Đang làm | Nền tảng | `Index.php`, `tra-cuu-sach.php` | DB query đã chạy; có phân trang 12 sách/trang; cần test trang kế và kiểm tra ảnh |
| Danh mục và sách | Đang làm | Nền tảng | `auth-quan-ly-sach.php`, `auth-nxb.php`, các trang thể loại/tác giả | CRUD danh mục, đầu sách và thao tác bản sao đã viết; xác nhận DB và hình ảnh còn lại |
| Độc giả và thẻ | Đang làm | Xác thực, DB | `auth-quan-ly-doc-gia.php` | Danh sách/tìm kiếm và luồng thẻ đã viết; chỉnh sửa hồ sơ và xác nhận DB còn lại |
| Mượn/trả và yêu cầu online | Đang làm | Độc giả, sách | `auth-muon-tra.php`, `auth-dang-ky-muon.php`, `auth-quan-ly-phieu-muon.php` | Mượn trực tiếp, đăng ký/hủy, duyệt/từ chối, nhận trả và lọc phiếu đang mở đã viết; lịch sử và xác nhận DB còn lại |
| Quản trị/hỗ trợ/thống kê | Đang làm | Xác thực, nghiệp vụ | `auth-thanh-ly-sach.php`, `auth-bao-cao-su-co.php`, `auth-lich-su-he-thong.php`, `thong-ke.php` | Thanh lý, báo cáo, nhật ký và thống kê đã viết; xác nhận DB còn lại |
| Giao diện và đối chiếu | Đang làm | Các module | Toàn bộ `views/`, `assets/` | Menu theo vai trò, assets và HTTP cơ bản đã kiểm tra; kiểm thử vai trò/giao diện đầy đủ còn lại |

## Rủi ro và điểm cần xác nhận khi triển khai

- Một số xử lý PHP dùng SQL nối chuỗi hoặc lưu mật khẩu dạng cũ; cần dùng truy vấn tham số và xác định lộ trình chuyển mật khẩu.
- Java đăng ký tài khoản mới bằng BCrypt, trong khi PHP hiện so khớp mật khẩu dạng văn bản; không dùng hai ứng dụng để tạo/đăng nhập cùng tài khoản mới trong giai đoạn chạy song song nếu chưa có bước chuyển đổi tương thích.
- Schema `QuanLyThuVien` đã được truy cập; cột `DocGia.MaDocGia` là số tự tăng và `TaiKhoan.MaNhom` là khóa số. Hai khác biệt này đã được sửa trong code. Tiếp tục kiểm tra các luồng ghi trước khi đưa vào dùng.
- Nghiệp vụ mượn/trả phải được khóa/transaction phù hợp để tránh hai người mượn cùng một bản sách.
- Tài liệu PHP hiện tại ghi DB `QuanLyThuVien`; cần xác nhận DB này là nguồn dữ liệu được dùng khi chạy.
- Giữ dự án PHP trong thời gian đối chiếu; chỉ chuyển vận hành sau khi người phụ trách xác nhận dữ liệu và chức năng.

## Nhật ký tiến độ

| Ngày | Cập nhật | Ghi chú |
|---|---|---|
| 2026-10-05 | Tạo kế hoạch và kiểm kê màn hình/action PHP; ghi nhận thư mục Java mới chỉ có bộ khung web/config cơ bản. | Chưa chuyển nghiệp vụ nào trong đợt này. |
| 2026-10-05 | Bắt đầu nền Java: thêm truy vấn đếm báo cáo chờ xử lý, luồng đăng nhập/đăng xuất, trang chủ và form Thymeleaf; MySQL chưa chạy nên schema và luồng DB chưa được xác nhận. | Chưa chạy build/test. Đăng ký, quản lý tài khoản, templates nghiệp vụ và đối chiếu DB còn chờ các bước sau. |
| 2026-10-05 | Bổ sung đăng ký độc giả có transaction và BCrypt; chuyển tra cứu sách theo tên/tác giả/thể loại, nhóm tác giả và số bản còn. | MySQL vẫn chưa chạy; chưa xác nhận schema, chạy build hoặc test. Quản lý tài khoản, giao diện chung và danh mục CRUD chưa làm. |
| 2026-10-05 | Thêm màn hình quản lý thể loại, tác giả, nhà xuất bản; truy vấn tham số hóa và kiểm tra liên kết trước khi xóa thể loại/NXB. | Mã đã viết nhưng chưa chạy với DB; luồng dữ liệu và ràng buộc khóa ngoại cần đối chiếu thực tế. |
| 2026-10-05 | Thêm trang quản lý đầu sách/bản sao: tạo đầu sách cùng liên kết tác giả, thêm bản sao, xóa bản chưa mượn, thanh lý bản từng mượn và chặn xóa đầu sách có lịch sử. | Dùng transaction và truy vấn tham số hóa; chưa chạy build hoặc đối chiếu schema vì MySQL đang tắt. |
| 2026-10-05 | Thêm danh sách/tìm kiếm độc giả, cấp thẻ một năm, gia hạn, khóa/mở khóa và hiển thị tiền cọc cùng thời hạn. | Chưa xác nhận schema/luồng trên DB; hồ sơ độc giả hiện mới đọc và tìm kiếm, chưa có chỉnh sửa. |
| 2026-10-06 | Chuyển đăng ký/hủy yêu cầu mượn online và hàng đợi duyệt/từ chối cho nhân viên; kiểm tra thẻ, quá hạn, hạn mức, trạng thái bản sách; duyệt tạo phiếu hạn 14 ngày theo transaction. | Chưa chạy build/test hoặc đối chiếu DB; cần tiếp tục với mượn trực tiếp và trả sách sau khi xác nhận schema. |
| 2026-10-06 | Thêm mượn trực tiếp cho nhân viên: kiểm tra lại thẻ, quá hạn, hạn mức và bản sách; tạo phiếu, chi tiết mượn và khóa bản sao trong một transaction. | Chưa đối chiếu DB hoặc chạy build/test; module trả sách vẫn chưa chuyển. |
| 2026-10-06 | Thêm danh sách phiếu đang mở, tìm kiếm/lọc theo hạn trả và nhận trả sách; tính phạt hỏng/mất/rách/quá hạn, cập nhật tình trạng/lịch sử bản sách và ghi nhật ký trong transaction. | Chưa xác nhận schema hoặc chạy build/test; cần kiểm tra thực tế phép tính và trạng thái khi MySQL hoạt động. |
| 2026-10-06 | Thêm thanh lý sách có nhật ký, báo cáo sự cố theo quyền độc giả/nhân viên và trang lịch sử hoạt động/mượn/thanh lý. | Chưa chạy build/test hoặc đối chiếu truy vấn với MySQL. |
| 2026-10-06 | Thêm trang thống kê đầu sách, độc giả, mượn hiện tại, tổng phạt, thanh lý và danh sách quá hạn; nút in dùng chức năng trình duyệt. | Chưa đối chiếu số liệu hoặc build/test do MySQL chưa chạy. |
| 2026-10-06 | Khởi động MySQL XAMPP và đối chiếu database thật: xác nhận 16 bảng, cột chính, nhóm vai trò, trạng thái mượn online; chạy Maven Test (1 test, 0 lỗi) và kiểm tra HTTP trang chủ/đăng nhập/catalog (200). | Catalog query hoạt động trên DB. Chưa thực hiện đăng nhập/thao tác ghi để tránh tạo hoặc sửa dữ liệu thật. |
| 2026-10-06 | Sửa đăng ký để dùng `MaDocGia` tự tăng; ánh xạ role name sang `NhomNguoiDung.MaNhom`; tạo hồ sơ `NhanVien`/`DocGia` khi quản trị tạo tài khoản; thêm phân trang catalog 12 đầu sách/trang. | Đã chạy lại Maven Test: BUILD SUCCESS, 1 test/0 lỗi; ứng dụng chạy lại, trang gốc/đăng nhập/trang lớn bất thường trả HTTP 200; catalog hiển thị 10/10 đầu sách khớp DB. Các màn hình ghi vẫn cần dữ liệu thử riêng. |
