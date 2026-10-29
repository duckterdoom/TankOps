# TankOps

**MacBook:** dùng `TankOps-Mac.zip`, giải nén và mở `Start-TankOps.command` sau khi cài Java 21+ cho Mac. Gói dùng được với Java phù hợp trên Mac Intel hoặc Apple Silicon. Có sẵn script tạo `.app` kèm Java trên Mac; xem [hướng dẫn macOS](docs/MACOS_SETUP.md).

Nhánh `test_main` chứa sẵn `TankOps.exe` cho người chơi Windows x64 và `TankOps-Ubuntu.zip` cho máy chủ Ubuntu. Sau khi clone hoặc tải ZIP mã nguồn và giải nén, có thể dùng ngay hai tệp này, không cần build lại. Người chơi chỉ cần `TankOps.exe`; địa chỉ server do người chạy server cung cấp.

Nhấp đúp **`TankOps.exe`** ngay ở thư mục gốc để mở game, không cần vào IDE.

Chỉ cần **một file `TankOps.exe`** để chơi hoặc gửi sang máy Windows x64 khác.
File này đã chứa Java, bản đồ, hình ảnh và âm thanh; người chơi không cần cài Java
hoặc Maven. Lần mở đầu tiên, game tự giải nén vào `%LOCALAPPDATA%\TankOps\versions`
(có thể mất vài giây); các lần sau dùng lại dữ liệu đã giải nén.
Mỗi bản build có bộ nhớ đệm riêng theo mã SHA-256 của nội dung game.

Nếu máy Windows báo lỗi khởi động, bản launcher mới ghi chi tiết tại
`%LOCALAPPDATA%\TankOps\logs`. Có thể chạy `./TankOps.exe --diagnose` trong PowerShell
để kiểm tra Java đi kèm mà không mở game. Gửi file `.log` mới nhất khi cần chẩn đoán;
không cần cài Java ngoài hoặc tắt Windows Security. Launcher Windows hiện dành cho x64.
Nếu đường dẫn tài khoản Windows có dấu, launcher thử tên đường dẫn ngắn tương thích;
nếu không có, dùng `.tankops-cache` cạnh EXE trong thư mục có đường dẫn không dấu và có quyền ghi.
Trong trường hợp đó log nằm tại `.tankops-cache/logs`. Không cần xóa cache cũ thủ công.

Chọn **ENTER GAME → Singleplayer** để chơi offline. Multiplayer cần server chạy riêng.

Để chơi qua các mạng khác nhau bằng Ubuntu VM + Tailscale, dùng gói server
`TankOps-Ubuntu.zip` và làm theo [hướng dẫn Tailscale](docs/TAILSCALE_SETUP.md).
Mỗi phòng tối đa **5 người**. Client nhớ tên và địa chỉ server trên máy người chơi;
chỉ cần nhập IP Tailscale hoặc tên DNS của Ubuntu, cổng mặc định là 5555.

Khi sửa hoặc kéo code mới, đóng game và chạy lại lệnh sau trong PowerShell tại thư mục dự án để cập nhật bản exe:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-TankOps.ps1
```

Để build cần JDK 21 trở lên và .NET Framework có sẵn trên Windows.
Script tự tìm JDK qua `JAVA_HOME`, `PATH`, thư mục `.jdks` và các thư mục cài đặt phổ biến.
Có thể chỉ định thủ công bằng `-JdkHome "C:\duong-dan\jdk-21"`.
Script chỉ đóng gói mã production của `tankops-common` và `tankops-client`,
kèm bản đồ, sprite và âm thanh; không cần Maven hoặc module database.
