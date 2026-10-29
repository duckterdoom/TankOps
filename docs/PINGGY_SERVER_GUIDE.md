# Hướng dẫn bật server TankOps và nhập địa chỉ Pinggy

Áp dụng khi server chạy trong Ubuntu VM trên máy Windows của chủ phòng. Người chơi ở mạng khác chỉ cần game, không cần cài Tailscale hoặc cấu hình router.

Điều kiện đã có: Ubuntu VM dùng NAT; chuyển tiếp SSH từ **127.0.0.1:2222 trên Windows** vào **cổng 22 của Ubuntu**; bộ server nằm trong `~/TankOps` và có Java 21+ hoặc runtime đi kèm.

## 1. Bật máy ảo và mở server

Khởi động Ubuntu VM. Trên **PowerShell Windows**, chạy:

```powershell
ssh -p 2222 tankops@127.0.0.1
```

Nhập mật khẩu Ubuntu. Nếu dùng username khác, thay `tankops` bằng username đó. Khi thấy dấu nhắc dạng `tankops@TankOps:~$`, bạn đã vào Ubuntu.

Nếu đang có server cũ chạy, nhấn **Ctrl+C** ở cửa sổ đó trước. Trong **phiên SSH Ubuntu**, dùng lệnh Java trực tiếp để chỉ định rõ địa chỉ; cách này không phụ thuộc phiên bản `start-server.sh`:

```bash
cd ~/TankOps
java -Xms64m -Xmx512m -Dtankops.config=./config.properties -Dtankops.bindAddress=127.0.0.1 -jar tankops-server.jar
```

Kết quả cần có:

```text
[GameServer] Đang lắng nghe tại 127.0.0.1:5555
```

**Chỉ tiếp tục mở Pinggy khi log đúng `127.0.0.1:5555`. Giữ cửa sổ server mở.**

Nếu dùng gói native có `runtime/bin/java` và máy chưa cài Java, dùng lệnh tương đương sau **thay cho**, không chạy thêm bên cạnh lệnh trên:

```bash
cd ~/TankOps
./runtime/bin/java -Xms64m -Xmx512m -Dtankops.config=./config.properties -Dtankops.bindAddress=127.0.0.1 -jar tankops-server.jar
```

Không chạy đồng thời hai lệnh mở server trên cùng cổng 5555.

### Tránh nhầm sang Tailscale

Nếu log hiện các dòng như sau thì bạn đang chạy script cũ/chế độ Tailscale:

```text
Ubuntu Tailscale IP: 100.x.x.x
[GameServer] Đang lắng nghe tại 100.x.x.x:5555
```

Script cũ có thể **bỏ qua `--local` mà vẫn khởi động**, nên chỉ thêm tùy chọn chưa đủ. Pinggy trong hướng dẫn này chuyển tiếp đến `127.0.0.1:5555`, không phải IP Tailscale.

Khi gặp trường hợp trên: **Ctrl+C → chạy lệnh Java trực tiếp ở bước 1 → xác nhận log localhost**. Không cần gỡ Tailscale. Bản `start-server.sh` mới có hỗ trợ `--local`, nhưng chỉ dùng sau khi đã chép/giải nén ZIP mới và kiểm tra log; không dùng `bash start-server.sh` không tham số cho Pinggy.

## 2. Mở đường hầm Pinggy

Mở **tab PowerShell Windows thứ hai**, SSH thêm một phiên:

```powershell
ssh -p 2222 tankops@127.0.0.1
```

Sau khi đăng nhập, chạy lệnh này **trong Ubuntu**:

```bash
ss -ltnp 'sport = :5555'
```

Kết quả cần có địa chỉ lắng nghe **`127.0.0.1:5555`**. Nếu không có dòng LISTEN, quay lại bước 1; nếu chỉ thấy IP Tailscale, sửa theo mục trên. Sau đó mới chạy:

```bash
ssh -p 443 -R0:127.0.0.1:5555 tcp@free.pinggy.io
```

Lần đầu SSH có thể yêu cầu xác nhận khóa máy chủ Pinggy. Kiểm tra đúng tên miền đang kết nối và xác minh fingerprint qua nguồn chính thức nếu cần trước khi chấp nhận.

Pinggy hiển thị địa chỉ tương tự:

```text
tcp://ten-may-chu.run.pinggy-free.link:12345
```

Đây chỉ là **ví dụ định dạng**. Lấy tên miền và cổng thực từ cửa sổ Pinggy của phiên đang chạy.

Thông báo “You are not authenticated” là chế độ không đăng nhập, không phải lỗi tạo tunnel. Phiên miễn phí thông báo thời hạn 60 phút; mở lại thường cấp địa chỉ mới. **Giữ cửa sổ Pinggy mở cùng cửa sổ server.**

## 3. Nhập địa chỉ vào game

Gửi địa chỉ của phiên Pinggy hiện tại cho tất cả người chơi. Trong ô **Server Address**, nhập:

```text
ten-may-chu.run.pinggy-free.link:12345
```

Quy tắc:

- **Bỏ `tcp://`**, không thêm `http://` hoặc `https://`.
- Giữ nguyên tên miền và cổng do Pinggy cấp.
- Cổng nhập trong game là cổng Pinggy, ví dụ **12345**, không tự đổi thành **5555**.
- Người chơi ở mạng khác không nhập `127.0.0.1` hoặc IP LAN của Ubuntu.

Trong game:

1. Nhập tên khác nhau cho từng người và địa chỉ server.
2. Chọn **ENTER GAME → Multiplayer**.
3. Cùng vào một phòng; cần ít nhất 2 người.
4. Các thành viên bấm **READY**, chủ phòng bấm **START GAME**.

Windows dùng `TankOps.exe`. Mac dùng gói đúng chip: `.app` kèm runtime không cần Java ngoài; gói JAR `TankOps-Mac.zip` vẫn cần Java 21+. Chi tiết tại [SYSTEM_ANALYSIS.md](../SYSTEM_ANALYSIS.md#4-gói-phát-hành-và-yêu-cầu-java).

## 4. Khi cần cập nhật bộ server

Không phải cập nhật mỗi lần bật máy. Chỉ làm khi đã có bản server/giao thức mới; đóng server bằng **Ctrl+C** trước khi thay file.

Trong **PowerShell Windows**, tại thư mục dự án:

```powershell
cd D:\TankOps
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Server.ps1 -JdkHome "C:\duong-dan\jdk-21"
scp -P 2222 .\TankOps-Ubuntu.zip tankops@127.0.0.1:~/
```

Thay đường dẫn JDK bằng thư mục cài thật. Đợi build báo `Ready` rồi mới chạy `scp`. Không chạy `scp D:\...` bên trong Ubuntu.

Trong **SSH Ubuntu**, giải nén đè và mở lại:

```bash
unzip -o ~/TankOps-Ubuntu.zip -d ~/TankOps
cd ~/TankOps
java -Xms64m -Xmx512m -Dtankops.config=./config.properties -Dtankops.bindAddress=127.0.0.1 -jar tankops-server.jar
```

Pinggy còn hoạt động thì giữ nguyên. Nếu đã tắt/hết hạn, chạy lại bước 2 và gửi địa chỉ mới. Nếu đổi wire format, mọi người phải dùng client cùng phiên bản với server.

Sau khi giải nén vẫn kiểm tra log phải là `127.0.0.1:5555`. Cập nhật file trong repo Windows chưa tự cập nhật file đang nằm trên Ubuntu.

## 5. Tắt và mở lại lần sau

1. **Ctrl+C** trong cửa sổ Pinggy.
2. **Ctrl+C** trong cửa sổ Java server.
3. Nếu muốn tắt Ubuntu, chạy `sudo poweroff`.

Lần sau lặp bước 1–3 của hướng dẫn này, không cần build/chép/unzip nếu code không đổi. Máy Windows chứa VM phải hoạt động và không Sleep khi chơi. Khi đổi mạng LAN, SSH NAT vẫn dùng `127.0.0.1:2222`; nếu tunnel mất kết nối thì tạo lại và dùng địa chỉ mới.

## 6. Kiểm tra nhanh khi không kết nối được

Trong **PowerShell Windows**, thay tên miền/cổng bằng địa chỉ thực:

```powershell
Test-NetConnection ten-may-chu.run.pinggy-free.link -Port 12345
```

Trong **phiên SSH Ubuntu khác với cửa sổ đang chạy server**:

```bash
ss -ltnp 'sport = :5555'
```

| Hiện tượng | Kiểm tra |
| --- | --- |
| SSH cổng 2222 bị từ chối | VM đang bật, sshd chạy và VirtualBox có luật NAT đúng máy ảo |
| Không có `~/TankOps` | Chưa chuyển ZIP/giải nén; xem bước 4 |
| Log hiện `Ubuntu Tailscale IP` dù đã thêm `--local` | Script trên Ubuntu còn cũ; dừng bằng Ctrl+C rồi chạy lệnh Java trực tiếp ở bước 1 |
| Không có LISTEN tại `127.0.0.1:5555` | Chưa mở server đúng đích của tunnel; kiểm tra log/bind trước khi vào game |
| `Address already in use` | Server cũ còn giữ cổng; Ctrl+C đúng cửa sổ cũ rồi mở lại một server |
| Pinggy hết hạn/đã tắt | Tạo lại tunnel, gửi địa chỉ mới |
| Có phòng nhưng START không vào trận | Đủ người/READY/host; kiểm tra client và server cùng protocol, đọc log server |
| Di chuyển còn trễ | Xem RTT trong game; kết nối thông không đồng nghĩa đường truyền có độ trễ thấp |

Tài liệu tổng hợp: [SYSTEM_ANALYSIS.md](../SYSTEM_ANALYSIS.md). Thời hạn dịch vụ: [Pinggy Help](https://pinggy.io/help/).
