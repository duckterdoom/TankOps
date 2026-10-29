# TankOps

Game xe tăng Java/Swing: chơi đơn offline và multiplayer TCP, tối đa 5 người/phòng.

## Mở game

- **Windows x64:** mở TankOps.exe, không cần cài Java. Giữ đủ thời gian giải nén lần đầu.
- **Mac chưa có Java:** dùng ZIP chứa TankOps.app, được build trên Mac bằng Build-Mac-Native.sh, đúng chip Intel/Apple Silicon.
- **TankOps-Mac.zip:** gói JAR phát triển, cần Java 21+ trên Mac; giải nén và mở Start-TankOps.command.
- **Tạo ZIP Mac không cần Java:** trên Mac có JDK 21+, giải nén TankOps-Mac.zip rồi chạy `bash Build-TankOps-App.command` trong thư mục vừa giải nén. Gửi file `TankOps-Mac-aarch64.zip` (Apple Silicon) hoặc `TankOps-Mac-x86_64.zip` (Intel) được tạo ở cùng thư mục cho người chơi; họ giải nén và mở TankOps.app.
- **Server Ubuntu:** TankOps-Ubuntu.zip cần Java 21+; bản native tar.gz được build trên Linux có runtime đi kèm.

Multiplayer: nhập tên khác nhau và địa chỉ server **host:port**, không thêm tcp://. Vào cùng phòng, tối thiểu 2 người; thành viên READY rồi chủ phòng START. Với Pinggy, chủ server cung cấp địa chỉ hiện tại và giữ server/tunnel hoạt động.

## Tài liệu

- [Hướng dẫn server và Pinggy](docs/PINGGY_SERVER_GUIDE.md): bật server, mở tunnel và nhập địa chỉ vào game từng bước.
- [SYSTEM_ANALYSIS.md](SYSTEM_ANALYSIS.md): hiện trạng, triển khai Ubuntu VM/Pinggy/Tailscale, build Windows/Mac/Linux, tối ưu mạng, kiểm chứng và chẩn đoán lỗi.
- [docs/protocol.md](docs/protocol.md): đặc tả TCP binary theo codec hiện tại.

## Build nhanh trên Windows

Cần JDK 21+ đầy đủ; thay đường dẫn bằng JDK thật. Đóng game trước khi ghi đè EXE.

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-TankOps.ps1 -JdkHome "C:\duong-dan\jdk-21"
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Server.ps1 -JdkHome "C:\duong-dan\jdk-21"
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Mac.ps1 -JdkHome "C:\duong-dan\jdk-21"
~~~

Windows build kiểm tra runtime, tài nguyên, khởi tạo Swing và Access Bridge trước khi xuất EXE. Có thể kiểm tra lại trên máy nhận:

~~~powershell
$p = Start-Process .\TankOps.exe -ArgumentList '--diagnose' -PassThru -Wait
$p.ExitCode
~~~

Mã 0 là self-test đạt. Log tại %LOCALAPPDATA%/TankOps/logs hoặc .tankops-cache/logs cạnh EXE. Kiểm tra này chưa thay thế thử game trên máy sạch và chơi nhiều máy khác mạng. Native Mac/Linux cần được chạy kiểm chứng trên OS đích.

Bản EXE và bản TankOps.app tự chứa luôn dùng Java đi kèm; máy có Java/JDK khác phiên bản vẫn chạy bằng runtime của game. Gói JAR TankOps-Mac.zip dùng Java của máy nên cần Java 21 trở lên. Không cần gỡ hay đổi Java đang dùng cho dự án khác.

Mỗi lần build EXE còn kiểm tra giải nén lần đầu khi PATH không có Java, mở lại với JAVA_HOME sai/các tùy chọn JVM không hợp lệ, và chạy EXE từ đường dẫn có dấu. Các kiểm tra này chạy trước khi thay file TankOps.exe phát hành.
