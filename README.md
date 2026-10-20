# A7_231A290036

Võ Văn Quốc Bảo · MSSV 231A290036 · INT4211 · 20/10/2026

## Tệp cần nộp

- **Project Android Studio:** toàn bộ thư mục gốc này (app/, gradle/, gradlew.bat và các tệp cấu hình).
- **Báo cáo PDF:** [BaoCao/BaoCao_A7_231A290036.pdf](BaoCao/BaoCao_A7_231A290036.pdf).
- **Bản báo cáo có thể chỉnh sửa:** BaoCao/BaoCao_A7_231A290036.md.
- **Ảnh minh chứng gốc:** AnhMinhChung/.
- **Video demo:** [Video/Demo_A7_231A290036.mp4](Video/Demo_A7_231A290036.mp4), 40.6 giây, không âm thanh, quay trực tiếp từ máy ảo.
- **APK cài thử:** [APK/A7_231A290036-debug.apk](APK/A7_231A290036-debug.apk).
- **Kết quả kiểm thử, Logcat và lint:** KiemThu/.
- **Repository Private:** https://github.com/VOVANQUOCBAO/A7_231A290036 . Không mời giảng viên theo yêu cầu của chủ tài khoản.

## Mở và chạy bằng Android Studio

1. Chọn **File → Open**, chọn chính thư mục **A7_231A290036** này, không chọn riêng thư mục app.
2. Chờ Gradle Sync. Dùng **Gradle JDK: Embedded JDK (jbr)** của Android Studio; máy này đã dùng JBR 25.0.3. Mã Java đặt source/target 17.
3. SDK Manager cần Android SDK Platform 35 và Build Tools 36.0.0 (môi trường đã cài). Nếu SDK ở đường dẫn khác, để Android Studio cập nhật local.properties.
4. Chọn module **app**, chọn thiết bị API 24 trở lên, bấm **Run**. A7 nên dùng API 33+ để thấy hộp thoại thông báo; bật camera Emulated trên máy ảo.
5. Muốn chỉ build: chạy `.\gradlew.bat :app:assembleDebug :app:lintDebug` tại thư mục gốc. Lần đầu trên máy khác cần mạng để tải Gradle và thư viện.

## Phần nâng cao

NC3: chạm thông báo mở lại app; NC4: sơ đồ luồng quyền trong báo cáo và tệp Mermaid.

## Kiểm chứng

Đã build APK, cài và chạy trên Android Emulator Medium Phone **API 36**. Lượt chính đạt **6 phép kiểm tra**. Các bước và kết quả cụ thể trong báo cáo; log thô nằm ở KiemThu/instrumentation_full.txt và KiemThu/logcat_full.txt. A7 còn có lượt `denial` và `bad_registration` riêng.

Chạy lại: mở máy ảo, mở PowerShell ở thư mục này và chạy `powershell -ExecutionPolicy Bypass -File .\KiemThu\Chay_KiemThu.ps1`. A7 dùng thêm `-Mode denial` và `-Mode bad_registration`; lượt full/denial xóa dữ liệu riêng của app A7 để thử quyền từ đầu. Để kết quả định dạng và nhãn hệ thống trùng minh chứng, dùng máy ảo ngôn ngữ English (US).

Các ca nhập ký tự sai ở A4 được runner đưa trực tiếp vào EditText để kiểm chứng xử lý dữ liệu độc lập với bàn phím số. Không có mã tự chạy demo hoặc cửa hậu kiểm thử trong ứng dụng chính. Runner nằm riêng trong app/src/androidTest/.

Android Lint: **0 lỗi**, còn cảnh báo về phiên bản phụ thuộc/SDK và một số gợi ý giao diện/tài nguyên; xem tệp lint đi kèm. Chưa kiểm chứng trên điện thoại thật hoặc trên mọi phiên bản Android từ API 24.

## Nguồn yêu cầu

Lab_A7_HuongDanThucHanh.pdf do người dùng cung cấp. Công nghệ: **Java + XML**, không dùng Compose/Kotlin cho mã nguồn ứng dụng.
