# Kiểm chứng bản phát hành TankOps

## Chọn đúng gói

| Gói | Người dùng cần Java cài sẵn? | Phạm vi |
| --- | --- | --- |
| `TankOps.exe` | Không | Windows x64; .NET Framework của Windows vẫn là phụ thuộc của launcher |
| `TankOps-Mac.zip` với `.command` + JAR | Có, Java 21+ | Gói phát triển, không phải bản tự chứa runtime |
| ZIP chứa `TankOps.app`, build bằng `Build-Mac-Native.sh` | Không | Mac đúng kiến trúc JDK dùng để build: Intel hoặc Apple Silicon |
| `TankOps-Ubuntu.zip` build trên Windows | Có, hoặc chạy setup để cài Java | Gói server JAR; không chứa runtime Linux |
| `TankOps-Ubuntu-<arch>.tar.gz`, build trên Linux bằng `Build-Server-Native.sh` | Không | Server trên Ubuntu/Linux tương thích kiến trúc và thư viện hệ điều hành của JDK dùng để build |

Java của Windows không dùng được trên macOS/Linux. Không đổi đuôi EXE thành APP. Máy build cần JDK đầy đủ; máy người chơi dùng bản tự chứa runtime không cần JDK/JRE ngoài.

## Windows: chặn bản build thiếu runtime trước khi phát

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-TankOps.ps1 -JdkHome "C:\duong-dan\jdk-21"
```

Script tự thực hiện trước khi ghi đè EXE phát hành:

1. Biên dịch source, đóng gói tài nguyên; dùng `jdeps` lấy module từ JAR thay vì duy trì danh sách tối thiểu thủ công.
2. Bổ sung `jdk.accessibility` là phụ thuộc động không thể suy ra chỉ từ import của game.
3. Chạy Java **trong app image**, không dùng Java hệ thống: bỏ JAVA_HOME, JVM option bên ngoài và chỉ giữ System32 trong PATH.
4. Kiểm tra cả trạng thái bình thường và ép bật Access Bridge. Self-test giải mã map/ảnh/âm thanh, protocol và tạo rồi hủy MainFrame Swing thật, không hiển thị cửa sổ hoặc bắt đầu trận.
5. Nhúng runtime vào EXE, chạy chính EXE với `--diagnose`; chỉ công bố `Ready` sau khi thành công.

`--diagnose` bây giờ kiểm tra khởi tạo ứng dụng, không chỉ `java -version`. Nó không yêu cầu Internet/server. Log tại `%LOCALAPPDATA%\TankOps\logs` hoặc `.tankops-cache\logs` nếu dùng cache dự phòng.

Kiểm tra lại trên máy nhận:

```powershell
$p = Start-Process .\TankOps.exe -ArgumentList '--diagnose' -PassThru -Wait
$p.ExitCode
```

Mã 0 là self-test đạt; khác 0 đọc log. Sau đó mở game bình thường và chơi thử. Không xóa file trợ năng của người dùng hay tắt Windows Security để che lỗi đóng gói.

## Mac: tạo ứng dụng có runtime trên macOS

Trên Mac của nhóm có JDK 21+ phù hợp chip:

```bash
bash Build-Mac-Native.sh
```

Script biên dịch source hiện tại, tạo JAR, dùng jdeps/jlink để tạo Java tối giản và jpackage tạo `.app`. Sau đó chạy **launcher trong `.app`** với `--self-test` khi bỏ Java ngoài khỏi môi trường. Chỉ đóng gói ZIP cuối khi kiểm tra thành công.

Máy nhận giải nén và mở `TankOps.app`, không phải cài Java. App chưa ký Developer ID/notarize: kiểm tra việc mở app tải từ Internet trên máy nhận; đây là bước khác với kiểm tra runtime. Không tuyên bố đã hỗ trợ mọi macOS chỉ vì build thành công trên một Mac.

## Ubuntu: lựa chọn server mang sẵn Java

Gói ZIP cũ vẫn dùng được khi Java 21 đã được cài. Để tạo gói tự chứa runtime, chạy **trên Ubuntu có JDK 21+**:

```bash
bash Build-Server-Native.sh
```

Gói tar.gz giữ quyền executable của Java. Sau khi giải nén trên máy nhận:

```bash
cd TankOps-Ubuntu
./runtime/bin/java -jar tankops-server.jar --self-test
bash start-server.sh --local
```

`--local` bind 127.0.0.1 cho Pinggy và không cần Tailscale. Không truyền tùy chọn thì launcher dùng IP Tailscale như trước. Launcher ưu tiên runtime đi kèm; không có mới dùng Java hệ thống. Gói native không tự cài Pinggy/Tailscale hoặc thay đổi firewall.

## Kiểm tra trước buổi demo

- Chạy ít nhất một lần trên máy sạch hoặc VM/snapshot chưa cài Java, đúng OS/chip của người nhận. Việc bỏ Java khỏi PATH trên máy build là phép thử có ích nhưng không thay thế hoàn toàn máy sạch.
- Đảm bảo không có `.tankops-cache`/cache bản đó từ lần chạy trước để thử cả giải nén lần đầu; dùng tài khoản thử nghiệm mới thay vì xóa dữ liệu người dùng tùy tiện.
- Thử tài khoản Windows có dấu và đường dẫn có khoảng trắng; kiểm tra log nếu hệ thống không cung cấp tên đường dẫn ngắn.
- Mở offline trước, kiểm tra hình ảnh, phím và âm thanh. Self-test mới kiểm tra giải mã âm thanh, không chứng minh loa/driver âm thanh hoạt động.
- Chơi multiplayer 2 máy khác mạng, cùng phiên bản; READY/START, di chuyển/bắn, kết thúc/rời phòng/vào ván tiếp.
- Lưu SHA-256 của EXE/ZIP/tar.gz đã kiểm chứng và phát đúng file đó. Không đổi source hoặc đóng gói lại rồi giữ kết quả kiểm thử của file cũ.

## Giới hạn kiểm chứng trong môi trường hiện tại

Có Windows để build và chạy kiểm tra runtime/giao diện ẩn. Không có macOS/Linux native trong môi trường này; các script native được kiểm tra cú pháp nhưng chưa được chạy trên các OS đó. Phải thực hiện các bước native trên máy tương ứng trước khi gọi bản Mac/Linux là đã nghiệm thu. Không có cách bảo đảm tuyệt đối mọi cấu hình máy; quy trình này giúp bắt lỗi đóng gói sớm và cung cấp bằng chứng kiểm thử rõ ràng.

Tham khảo: [jdeps](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jdeps.html), [jpackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html), [Accessibility properties](https://docs.oracle.com/en/java/javase/21/access/accessibility-properties.html).
