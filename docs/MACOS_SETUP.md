# Chạy TankOps trên MacBook

`TankOps.exe` chỉ dành cho Windows. Trên macOS dùng gói `TankOps-Mac.zip`.
Gói này chứa cùng mã client, bản đồ, sprite, âm thanh và giao thức multiplayer với bản Windows cùng lần build.

## Chơi bằng gói ZIP

1. Tải `TankOps-Mac.zip`, giải nén toàn bộ vào một thư mục. Giữ các file cạnh nhau.
2. Cài **Temurin JDK 21** cho macOS nếu chưa có Java 21 trở lên:
   https://adoptium.net/temurin/releases/?version=21&os=mac
   - Mac chip Apple M1/M2/M3/M4 hoặc dòng Apple Silicon khác: chọn **aarch64 / ARM64**.
   - Mac Intel: chọn **x64**.
   - Chọn trình cài `.pkg`, cài xong mới mở game.
3. Nhấp đúp `Start-TankOps.command`. Launcher sẽ mở Terminal và chạy game.
4. Nhập tên và địa chỉ server hiện tại, ví dụ `ten-server.run.pinggy-free.link:12345`, không thêm `tcp://`.
5. Chọn Multiplayer, cùng vào phòng; tối thiểu 2 người, thành viên READY rồi chủ phòng START.

Không cần Maven, IDE hay Tailscale khi server được công khai bằng Pinggy. Một gói JAR dùng cho cả Intel/Apple Silicon, nhưng Java cài trên Mac phải phù hợp chip và phiên bản macOS. Cả nhóm và server cần cùng phiên bản build.

Nếu Finder báo không có quyền chạy file, mở Terminal, gõ `chmod +x ` rồi kéo file `Start-TankOps.command` vào cửa sổ và Enter. Sau đó mở lại file. Cũng có thể chạy trực tiếp bằng `bash` rồi kéo file `.command` vào sau dấu cách.

Nếu macOS chặn file tải từ Internet, chỉ mở khi đã kiểm tra nguồn tải: thử Control-click → Open; nếu vẫn bị chặn, xem System Settings → Privacy & Security → Open Anyway (nếu có). Không tắt Gatekeeper toàn hệ thống. Nếu báo file hỏng hoặc độc hại, dừng và kiểm tra lại nguồn/file thay vì bỏ qua cảnh báo.

## Tạo TankOps.app có sẵn Java

Gói ZIP phía trên cần Java trên Mac. Nếu muốn chia sẻ ứng dụng `.app` để máy nhận không phải cài Java:

1. Trên một máy Mac đã cài **JDK 21+ đầy đủ**, giải nén gói ZIP.
2. Trong Terminal, gõ `bash ` rồi kéo `Build-TankOps-App.command` vào, Enter.
3. Script hiển thị đường dẫn `TankOps.app` và `TankOps-App.zip` trong thư mục `app-build.*`.
4. Chép `.app` vào Applications để chạy hoặc gửi `TankOps-App.zip` cho máy Mac tương thích kiến trúc JDK đã dùng.

Ứng dụng này chưa ký Developer ID/notarize, nên macOS có thể yêu cầu xác nhận nguồn như trên. Script không tạo bản universal Intel + ARM64; nên build riêng bằng JDK tương ứng nếu cần cả hai kiến trúc.

Không thể dùng `jpackage` của Windows để tạo runtime macOS. Tài liệu: https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html

## Cập nhật bản Mac từ Windows

Chạy tại thư mục repo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Mac.ps1 -JdkHome "C:\duong-dan\jdk-21"
```

Gửi `TankOps-Mac.zip` mới cho người dùng Mac khi cập nhật code, và cập nhật server cùng phiên bản. Không cần chuyển `TankOps.exe` sang Mac.

## Giới hạn kiểm chứng

Gói JAR được biên dịch và kiểm tra cấu trúc trên Windows. Chưa chạy GUI, âm thanh hoặc `jpackage` trên macOS trong môi trường này; cần test thực tế trên MacBook trước khi xem là bản macOS đã nghiệm thu.
