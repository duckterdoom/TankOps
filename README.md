# TankOps

Game xe tăng 2D viết bằng Java/Swing, hỗ trợ chơi đơn offline và multiplayer tối đa 5 người/phòng.

## Windows

Mở **[TankOps.exe](TankOps.exe)** — không cần cài Java. Lần đầu mở, chờ game giải nén dữ liệu.

## macOS

Giải nén **[TankOps-Mac.zip](TankOps-Mac.zip)**, cài **Java 21+** đúng chip Mac rồi mở **Start-TankOps.command**. Giữ nguyên các tệp trong thư mục đã giải nén.

Nếu nhận bản ZIP chứa **TankOps.app** kèm Java, chỉ cần giải nén và mở app; chọn đúng bản Apple Silicon (`aarch64`) hoặc Intel (`x86_64`).

## Bắt đầu chơi

- **Chơi đơn:** chọn Singleplayer, không cần server.
- **Chơi cùng bạn:** chủ server bật server và gửi địa chỉ `host:port` (bỏ `tcp://` nếu có). Nhập tên khác nhau, vào cùng phòng; tối thiểu 2 người, thành viên bấm **READY**, chủ phòng bấm **START**.

Xem [hướng dẫn server/Pinggy](docs/PINGGY_SERVER_GUIDE.md) và [tài liệu hệ thống](SYSTEM_ANALYSIS.md).
