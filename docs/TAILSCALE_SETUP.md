# Chơi TankOps qua Tailscale: Ubuntu VM + tối đa 5 người/phòng

Server chạy trong Ubuntu VM trên máy cá nhân. Mỗi người cài Tailscale trên Windows,
mở `TankOps.exe` rồi kết nối đến Ubuntu. Mỗi phòng tối đa **5 người**, bắt đầu từ
2 người; để test đủ 5, chờ đủ thành viên trước khi chủ phòng bấm START.
Không cần chỉnh modem/router hoặc biết IP public.

## 1. Chuẩn bị Ubuntu VM

Dùng Ubuntu 24.04 LTS hoặc bản Ubuntu được hỗ trợ có gói OpenJDK 21.
Khởi đầu có thể cấp 2 vCPU, 2 GB RAM; đây là cấu hình thử nghiệm, cần theo dõi khi chơi thực tế.
Để network adapter của VM ở chế độ NAT có Internet là được; Tailscale được cài
trực tiếp trong Ubuntu, không chỉ trên máy Windows chứa VM.

Chép `TankOps-Ubuntu.zip` vào thư mục Downloads của Ubuntu, mở Terminal rồi chạy:

```bash
sudo apt-get update
sudo apt-get install -y unzip
mkdir -p ~/TankOps
unzip ~/Downloads/TankOps-Ubuntu.zip -d ~/TankOps
cd ~/TankOps
bash setup-ubuntu.sh
```

Script cài Java 21 và Tailscale từ trình cài chính thức, bật dịch vụ Tailscale,
rồi hiển thị đường dẫn đăng nhập. Mở đường dẫn đó trong trình duyệt và đăng nhập
tài khoản Tailscale của bạn để xác nhận Ubuntu VM.

Nguồn: https://tailscale.com/docs/install/linux

## 2. Mở server

Trong thư mục `~/TankOps`, chạy:

```bash
bash start-server.sh
```

Script tự lấy IP Tailscale của Ubuntu và chỉ lắng nghe trên địa chỉ đó.
Ví dụ log `100.80.10.20:5555` thì client nhập chính địa chỉ này.
Các địa chỉ trong hướng dẫn là ví dụ; lấy IP thực bằng `tailscale ip -4`.
Giữ terminal, Ubuntu VM và máy chứa VM hoạt động trong lúc chơi.
Ctrl+C để dừng; lần sau chỉ chạy lại `bash start-server.sh`.

Gói mặc định có 1 phòng chờ tạo sẵn, tối đa 3 phòng và 5 người/phòng.
Chỉnh `config.properties` cạnh JAR rồi khởi động lại server nếu cần.

## 3. Cấp quyền cho người chơi

Mỗi người dùng tài khoản Tailscale riêng, không chia sẻ mật khẩu.
Chủ server vào https://console.tailscale.com/admin/machines, tìm Ubuntu VM,
chọn menu `...` → Share, sao chép link mời và tự gửi cho thành viên nhóm.
Mỗi người đăng nhập và chấp nhận lời mời để truy cập máy Ubuntu được chia sẻ.

Nếu nhóm đã ở cùng một tailnet, đảm bảo chính sách mạng cho phép các máy Windows
truy cập Ubuntu qua TCP 5555. Máy được chia sẻ có thể dùng tên đầy đủ
`ten-may.ten-tailnet.ts.net`; cũng có thể lấy IP hiển thị ở máy người nhận.

Nguồn: https://tailscale.com/docs/use-cases/personal-or-at-home-use/share-private-game-server

## 4. Mỗi máy Windows

1. Cài Tailscale: https://tailscale.com/download/windows
2. Đăng nhập tài khoản riêng, chấp nhận link chia sẻ server và để Tailscale kết nối.
3. Mở `TankOps.exe`, nhập tên khác nhau cho từng người.
4. Ô Server Address: nhập IP Tailscale Ubuntu, ví dụ `100.80.10.20:5555`.
   Có thể bỏ `:5555`; game tự thêm cổng mặc định và nhớ địa chỉ cho lần sau.
5. ENTER GAME → Multiplayer → cùng vào `room-1` (hoặc cùng phòng do một người tạo).
6. Bốn thành viên bấm READY, chủ phòng bấm START GAME.

Chỉ phát `TankOps.exe` cho người chơi. `TankOps-Ubuntu.zip` dành cho máy chủ.
Singleplayer vẫn chơi được offline.

## 5. Khi không kết nối được

Trên Windows, dùng IP thực của Ubuntu:

```powershell
tailscale status
tailscale ping 100.80.10.20
Test-NetConnection 100.80.10.20 -Port 5555
```

Trên Ubuntu:

```bash
tailscale status
tailscale ip -4
ss -ltnp 'sport = :5555'
```

Nếu ping được nhưng TCP không được, kiểm tra server đang chạy, địa chỉ lắng nghe,
quyền chia sẻ/chính sách Tailscale và firewall. Nếu dùng UFW và cần thêm luật,
chỉ cho phép cổng game trên giao diện Tailscale:

```bash
sudo ufw allow in on tailscale0 to any port 5555 proto tcp
```

Script không tự bật hoặc thay đổi UFW. Tailscale có thể quản lý luật iptables riêng,
nên cần kiểm tra cả chính sách Tailscale; không mở cổng game trên router nhà.

`tailscale ping` cho biết kết nối trực tiếp hay qua relay. Khi có độ trễ cao,
ưu tiên đường truyền ổn định/cắm dây mạng cho máy chứa VM và kiểm tra kiểu kết nối.
Không đổi MTU/DNS của router để xử lý game.

Nguồn: https://tailscale.com/docs/reference/connection-types

## 6. Kiểm tra cả nhóm

- 5 người vào cùng phòng, thấy đủ 5 tên và 5 xe.
- Người thứ 6 bị server từ chối vào phòng đầy.
- Cả nhóm thấy di chuyển, bắn, mất máu và kết quả nhất quán.
- Một người thoát: xe của người đó được bỏ khỏi trận, những người còn lại tiếp tục chơi.
- Kết thúc, mọi người rời phòng rồi vào phòng chờ để chơi ván tiếp.

## Build lại trên Windows

Tại thư mục dự án (cần JDK 21+):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Server.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-TankOps.ps1
```

Hai đầu phải dùng cùng phiên bản code/giao thức. Đóng game/server trước khi cập nhật.
Các kiểm thử socket trên máy phát triển không thay thế việc thử thực tế qua Tailscale.
