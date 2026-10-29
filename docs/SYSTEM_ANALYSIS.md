# TankOps — Phân tích hệ thống và hướng dẫn triển khai

*Cập nhật: 29/10/2026. Đối chiếu với mã nguồn hiện tại; thay thế báo cáo tiến độ ngày 26/10 và các hướng dẫn triển khai rời rạc.*

## 1. Phạm vi và trạng thái thực tế

TankOps là game xe tăng Java 21+, Swing, TCP binary, theo kiến trúc **server-authoritative**. Máy chủ quyết định vị trí thật, va chạm, đạn, sát thương, điểm và kết thúc trận. Client có nội suy và dự đoán **hình ảnh** có giới hạn để giảm cảm giác trễ.

| Module | Hiện trạng |
| --- | --- |
| tankops-common | Model OOP, engine, bản đồ, AI cho offline, power-up, codec và writer mạng có giới hạn |
| tankops-server | TCP acceptor/session, lobby/phòng thật, READY/START, tick mặc định 30 Hz, snapshot, điểm/kill/death và kết thúc/chơi lại |
| tankops-client | Singleplayer và multiplayer Swing; tạo/vào/rời phòng, kết quả, đồ họa, âm thanh, nội suy, RTT và chẩn đoán khởi động |
| database | Module MongoDB độc lập: MongoDatabaseManager, PlayerRepository, MatchRepository và test riêng |
| tankops-tools | Có pom.xml; chưa có BotClient/BenchmarkRunner |

Không dùng tỷ lệ hoàn thành ước lượng như bằng chứng nghiệm thu. Module database hiện dùng MongoDB, **không phải SQLite JDBC** như kế hoạch cũ. Server/client đang đóng gói không phụ thuộc module này; chưa nối lưu trận tự động vào GameRoom. Không coi saveMatch nhiều bước hiện tại là transaction ACID/rollback đã hoàn thành.

Tài liệu còn lại:
- [README](README.md): cách mở game nhanh.
- Tài liệu này: hiện trạng, triển khai, build, kiểm chứng và việc còn lại.
- [Giao thức binary](docs/protocol.md): ID và thứ tự trường trên đường truyền.

## 2. Kiến trúc và luồng chạy

~~~mermaid
flowchart LR
    Input[Phím và chuột] --> Controller[MultiplayerController]
    Controller --> CW[Writer nền client]
    CW --> TCP[TCP trực tiếp hoặc Pinggy/Tailscale]
    TCP --> Session[ClientSession]
    Session --> Queue[Hàng đợi input của phòng]
    Queue --> Tick[TickLoop 30 Hz và GameEngine]
    Tick --> SW[Writer riêng từng session]
    SW --> TCP
    TCP --> State[NetworkClient và GameStateStore]
    State --> Render[RenderInterpolator và LocalTankPreview]
    Render --> Swing[GamePanel render khoảng 60 FPS]
~~~

- ConnectionAcceptor nhận socket; mỗi ClientSession có luồng đọc riêng và bật TCP_NODELAY.
- Luồng nhận chỉ xếp input; TickLoop là bên cập nhật engine trong trận.
- MessageWriter giữ thứ tự, queue tối đa 32 bản tin/kết nối; ghi mạng không chặn EDT hoặc tick.
- Queue đầy thì đóng kết nối đó, không âm thầm bỏ START/GAME OVER/âm thanh hoặc tích lũy vô hạn. Client nhận quá chậm vẫn có thể bị ngắt.
- GameStateStore lưu snapshot authoritative; render không sửa dữ liệu server.
- GamePanel không dựng lại bản đồ khi dữ liệu không đổi; panel sảnh/phòng ẩn không dựng lại danh sách ở mỗi snapshot.

### Gameplay và phòng

Model gồm Entity/Vector2D/Direction/Bullet; Tank/PlayerTank và bốn loại EnemyTank. Địa hình gồm gạch, thép, nước, cây và băng. Power-up gồm Star, Helmet, Grenade, Shovel, Timer và TankPowerUp; AI offline dùng Patrol/Chase/Attack.

MapLoader nạp map_single.map và map_multi.map từ resources. Multiplayer dùng người chơi thật, không tự chèn bot.

Luồng multiplayer:
1. Đăng nhập bằng tên và địa chỉ host:port.
2. Client xin danh sách phòng, tạo phòng bằng roomId=new hoặc vào phòng cụ thể/auto.
3. Server từ chối phòng đầy, tên trùng và trạng thái không phù hợp; RoomStateMessage gửi danh sách người/host/READY.
4. Tối thiểu 2 người; thành viên READY, host START.
5. READY/UNREADY và /start hiện truyền bằng ChatMessage, chưa có StartGameMessage riêng.
6. Server gửi snapshot liên tục, rồi GameOverMessage với điểm/kill/death.
7. Rời phòng/kết quả xóa trạng thái cũ; có cơ chế đưa phòng FINISHED về LOBBY để chơi lại.

## 3. Tối ưu multiplayer đã có

| Thay đổi | Cơ chế và giới hạn |
| --- | --- |
| Gửi input sớm | Gửi ngay khi phím thay đổi; giữ bắn khoảng 30 Hz, trạng thái không đổi gửi lại khoảng 100 ms |
| Render đều | Timer 16 ms độc lập với nhịp nhận gói |
| Nội suy từ xa | RenderInterpolator giữ tối đa 12 frame, đệm 66 ms, dùng nanoTime; tránh blend qua đổi hướng/respawn/dịch chuyển lớn |
| Phản hồi xe mình | LocalTankPreview theo input, giới hạn lệch 12 px; dùng tốc độ player mặc định ở server 30 Hz |
| Bảo toàn gameplay | Preview kiểm tra AABB, tường/nước/biên; không dự đoán quán tính băng; không sửa vị trí authoritative hoặc kết quả bắn |
| Mạng ngừng cập nhật | Không tiến preview thêm sau 250 ms thiếu snapshot; hiển thị cảnh báo |
| Đo thực tế | Ping/Pong mỗi giây, RTT ở góc màn chơi; bao gồm chờ queue và xử lý ứng dụng |
| Nhả phím | Xóa trạng thái khi mất focus/dừng ván để tránh kẹt di chuyển/bắn |
| Âm thanh | Queue hữu hạn 24 tác vụ, hai worker; giới hạn âm trượt lặp lại, đóng stream và hạn chế log lỗi lặp |

Preview là phản hồi hiển thị có giới hạn, **chưa phải prediction/reconciliation đầy đủ với sequence ACK**. Có thể thấy chỉnh vị trí nhỏ khi server xác nhận. Nếu đổi tick rate khỏi 30, cần đánh giá lại preview và mô phỏng; engine hiện di chuyển theo bước mỗi tick nên tăng tick có thể đổi tốc độ gameplay.

Hai đợt kiểm tra Ping/Pong qua Pinggy trong phiên hỗ trợ có RTT trung vị khoảng 103–112 ms, đỉnh khoảng 183–193 ms, mỗi đợt chỉ 12 mẫu. Đây không phải benchmark hay RTT của mọi người chơi. Code không loại bỏ được độ trễ relay; nội suy tăng độ mượt nhưng thêm thời gian đệm cho vật thể từ xa.

## 4. Gói phát hành và yêu cầu Java

| Gói | Java ngoài trên máy nhận | Build bằng |
| --- | --- | --- |
| TankOps.exe | Không; Windows x64 và .NET Framework của Windows | Build-TankOps.ps1 trên Windows |
| TankOps-Mac.zip: JAR + .command | **Có**, Java 21+ đúng chip | Build-Mac.ps1 trên Windows |
| ZIP chứa TankOps.app | Không; đúng kiến trúc Mac/JDK | Build-Mac-Native.sh trên macOS |
| TankOps-Ubuntu.zip | **Có**, Java 21+ hoặc cài qua setup | Build-Server.ps1 trên Windows |
| TankOps-Ubuntu-<arch>.tar.gz | Không; Linux/CPU/thư viện hệ điều hành tương thích | Build-Server-Native.sh trên Linux |

Java Windows không chạy trên Mac/Linux. Máy build cần JDK đầy đủ; máy nhận gói tự chứa không cần JDK/JRE ngoài. Mac Intel và Apple Silicon cần bản native riêng. App Mac hiện chưa ký Developer ID/notarize.

Máy nhận đã cài Java/JDK khác phiên bản vẫn dùng runtime của gói EXE/.app; không cần đổi Java hệ thống. Riêng gói JAR TankOps-Mac.zip dùng Java ngoài nên cần Java 21+. Launcher Windows loại bỏ các tùy chọn JVM từ môi trường ứng dụng khác trước khi khởi động.

### Build trên Windows

Đóng EXE đang dùng trước khi ghi đè. Tại thư mục repo:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-TankOps.ps1 -JdkHome "C:\duong-dan\jdk-21"
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Server.ps1 -JdkHome "C:\duong-dan\jdk-21"
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Mac.ps1 -JdkHome "C:\duong-dan\jdk-21"
~~~

Find-Jdk.ps1 tìm JDK qua tham số, JAVA_HOME, PATH và thư mục cài phổ biến. Build chỉ lấy production source common + client/server tương ứng, không cần database/Maven để tạo các gói này.

Windows dùng jdeps xác định module, jlink tạo runtime và bổ sung jdk.accessibility vì phụ thuộc động này không được suy ra từ import. Launcher C# tự giải nén theo hash vào cache rồi gọi Java đi kèm.

### MacBook

Để tạo bản **không cần Java trên máy người chơi**, chạy trên Mac của nhóm có JDK 21+ đúng chip:

~~~bash
bash Build-Mac-Native.sh
~~~

Script biên dịch source, tạo runtime bằng jdeps/jlink, đóng gói bằng jpackage và chạy chính launcher .app với --self-test khi bỏ Java ngoài khỏi môi trường. Gói kết quả TankOps-Mac-aarch64.zip hoặc TankOps-Mac-x86_64.zip chứa TankOps.app; người nhận giải nén và mở app.

Nếu chỉ có TankOps-Mac.zip:

Trên Mac đã có JDK 21+, giải nén gói rồi chạy `bash Build-TankOps-App.command` trong thư mục đó. Script kiểm tra chính launcher .app bằng runtime đi kèm và xuất thêm ZIP tên cố định ngay trong thư mục: TankOps-Mac-aarch64.zip hoặc TankOps-Mac-x86_64.zip. Gửi ZIP này cho người chưa cài Java. Người nhận giải nén và mở TankOps.app; không cần chạy script build. Bản JAR bạn đã thử thành công không thay thế bước thử .app tự chứa trên Mac đích.
1. Cài [Temurin JDK 21 cho macOS](https://adoptium.net/temurin/releases/?version=21&os=mac): ARM64/aarch64 cho Apple Silicon, x64 cho Intel.
2. Giải nén toàn bộ, giữ JAR và launcher cùng thư mục.
3. Mở Start-TankOps.command. Nếu thiếu quyền chạy, dùng Terminal: chmod +x rồi kéo đường dẫn file vào.
4. Muốn tạo .app từ JAR đã có, chạy bash rồi kéo Build-TankOps-App.command vào; phải dùng JAR mới nhất.

Nếu macOS chặn file tải từ Internet, kiểm tra đúng nguồn rồi dùng Control-click → Open hoặc Privacy & Security → Open Anyway nếu hệ thống cho phép. Không tắt Gatekeeper toàn hệ thống. File bị báo hỏng/độc hại cần được kiểm tra lại.

### Ubuntu native

Trên Ubuntu có JDK 21+:

~~~bash
bash Build-Server-Native.sh
~~~

Chuyển tar.gz sang máy Ubuntu tương thích, giải nén giữ nguyên quyền executable:

~~~bash
tar -xzf TankOps-Ubuntu-amd64.tar.gz
cd TankOps-Ubuntu
./runtime/bin/java -jar tankops-server.jar --self-test
./runtime/bin/java -Xms64m -Xmx512m -Dtankops.config=./config.properties -Dtankops.bindAddress=127.0.0.1 -jar tankops-server.jar
~~~

Launcher ưu tiên runtime đi kèm; nếu không có mới tìm Java hệ thống. --local bind localhost cho Pinggy; không truyền tùy chọn thì dùng Tailscale.

## 5. Ubuntu VM và SSH từ Windows

Cấu hình thử nghiệm: Ubuntu Server 24.04 LTS, 2 vCPU, 2 GB RAM, đĩa động 25 GB. Đây là cấu hình khởi đầu, chưa phải tải đã nghiệm thu.

VirtualBox:
- Adapter 1: NAT, bật Cable Connected.
- Port Forwarding SSH: TCP; Host IP 127.0.0.1; Host Port 2222; Guest IP để trống; Guest Port 22.
- Cài Ubuntu thủ công nếu Unattended Installation báo username dành riêng; dùng username tankops hoặc tên riêng hợp lệ, chọn Install OpenSSH server.

Trong Ubuntu, nếu SSH chưa có:

~~~bash
sudo apt-get update
sudo apt-get install -y openssh-server
sudo systemctl enable --now ssh
~~~

Trong **PowerShell Windows**, không phải trong phiên Ubuntu:

~~~powershell
ssh -p 2222 tankops@127.0.0.1
scp -P 2222 .\TankOps-Ubuntu.zip tankops@127.0.0.1:~/
~~~

ssh dùng -p thường, scp dùng -P hoa. 127.0.0.1 là máy đang chạy lệnh: cổng 2222 chỉ nằm phía Windows. Đổi dây/router có thể đổi IP LAN Windows, nhưng SSH NAT 127.0.0.1:2222 vẫn như cũ khi VM đang chạy.

## 6. Chơi khác mạng qua Pinggy

Phương án đang dùng: người chơi chỉ mở client, không cài Tailscale, không cấu hình router. Máy chủ tạo kết nối TCP đi ra tới Pinggy.

### Cài/cập nhật ZIP server

Trong SSH Ubuntu:

~~~bash
sudo apt-get update
sudo apt-get install -y unzip openjdk-21-jre-headless
mkdir -p ~/TankOps
unzip -o ~/TankOps-Ubuntu.zip -d ~/TankOps
cd ~/TankOps
java -Xms64m -Xmx512m -Dtankops.config=./config.properties -Dtankops.bindAddress=127.0.0.1 -jar tankops-server.jar
~~~

Chỉ mở Pinggy sau khi log xác nhận **127.0.0.1:5555**. Dùng lệnh Java trực tiếp phía trên để tránh script cũ bỏ qua --local và vẫn bind Tailscale. Nếu gói có Java đi kèm nhưng máy chưa cài Java, thay lệnh java bằng ./runtime/bin/java.

Script start-server.sh mới có hỗ trợ --local, nhưng bản đã chép trên Ubuntu có thể chưa được cập nhật. Nếu log hiện Ubuntu Tailscale IP hoặc 100.x.x.x:5555: Ctrl+C, rồi mở lại bằng lệnh Java trực tiếp. Không cần gỡ Tailscale.

Ở phiên SSH Ubuntu thứ hai, kiểm tra:

~~~bash
ss -ltnp 'sport = :5555'
~~~

Phải thấy LISTEN tại 127.0.0.1:5555. Giữ cửa sổ server mở; trong phiên SSH thứ hai chạy:

~~~bash
ssh -p 443 -R0:127.0.0.1:5555 tcp@free.pinggy.io
~~~

Pinggy hiển thị địa chỉ dạng tcp://ten-may.run.pinggy-free.link:12345. Người chơi nhập **ten-may.run.pinggy-free.link:12345**, bỏ tcp:// và giữ đúng cổng; không nhập 127.0.0.1 hoặc cổng 5555 của Ubuntu vào máy ở mạng khác.

Phiên miễn phí hiện thông báo hết hạn sau 60 phút và cấp địa chỉ mới khi mở lại. Không lưu địa chỉ tạm của một phiên thành cấu hình cố định trong source. [Nguồn Pinggy](https://pinggy.io/help/).

Giữ Windows, VM, server và Pinggy hoạt động. Đổi mạng hoặc máy ngủ có thể làm tunnel mất kết nối; chạy lại và gửi địa chỉ mới.

### Cập nhật và tắt

- Sửa chỉ client/launcher hoặc dự đoán hiển thị: cập nhật EXE/gói Mac, không bắt buộc thay server nếu wire format không đổi.
- Sửa server: build ZIP, dừng Java bằng Ctrl+C, scp từ Windows, unzip -o và mở lại server.
- Sửa giao thức common: cập nhật **cả server và mọi client**. Lobby tương thích không chứng minh snapshot trận đấu tương thích.
- Pinggy còn sống thì không cần tạo lại chỉ vì restart Java.
- Tắt gọn: Ctrl+C Pinggy, Ctrl+C server, rồi sudo poweroff.
- Bật lại không cần build/unzip nếu source không đổi, nhưng tunnel miễn phí mở lại có địa chỉ mới.

### Cấu hình server

| Thuộc tính | Resources mặc định | Gói Ubuntu |
| --- | ---: | ---: |
| server.port | 5555 | 5555 |
| server.maxClients | 100 | 16 |
| room.maxPlayers | 5 | 5 |
| server.maxRooms | 10 | 3 |
| server.defaultRooms | 3 | 1 |
| game.tickRate | 30 | 30 |

game.multiMap=/maps/map_multi.map. Cấu hình ngoài qua -Dtankops.config; địa chỉ bind qua -Dtankops.bindAddress. Các giới hạn trên là cấu hình, không phải kết quả benchmark.

## 7. Tailscale: phương án thay thế

Nếu chấp nhận cài Tailscale trên mỗi máy:
1. Trong thư mục server Ubuntu, chạy bash setup-ubuntu.sh để cài Java/Tailscale và đăng nhập.
2. Lấy IP bằng tailscale ip -4; chạy bash start-server.sh không có --local.
3. Máy người chơi cài Tailscale, cùng mạng Tailscale hoặc nhận quyền chia sẻ máy Ubuntu từ chủ server.
4. Nhập địa chỉ Tailscale của Ubuntu và cổng 5555. Dùng tài khoản riêng, không chia sẻ mật khẩu.

SSH qua NAT vẫn dùng được dù Windows chưa có Tailscale; SSH/game qua IP Tailscale thì Windows cũng phải kết nối Tailscale. IP Tailscale thường giữ nguyên khi đổi mạng LAN; kết nối đang chơi vẫn có thể gián đoạn.

Kiểm tra:

~~~powershell
tailscale status
tailscale ping <IP-TAILSCALE>
Test-NetConnection <IP-TAILSCALE> -Port 5555
~~~

~~~bash
tailscale status
tailscale ip -4
ss -ltnp 'sport = :5555'
sudo ufw status
~~~

Chỉ nếu UFW đang chặn và cần luật, cho phép game trên giao diện Tailscale:

~~~bash
sudo ufw allow in on tailscale0 to any port 5555 proto tcp
~~~

[Linux install](https://tailscale.com/docs/install/linux), [chia sẻ game server](https://tailscale.com/docs/use-cases/personal-or-at-home-use/share-private-game-server). Kiểm tra điều khoản/gói hiện hành nếu dùng dịch vụ lâu dài.

## 8. Kiểm chứng trước khi phát hành

### Kiểm tra tự động

Build Windows phải đạt trước khi xuất EXE:
1. Biên dịch và kiểm tra module bằng jdeps.
2. Dùng đúng runtime đóng gói với JAVA_HOME/Java ngoài bị loại khỏi môi trường.
3. Đọc map, giải mã ảnh/âm thanh, round-trip protocol và tạo/hủy MainFrame thật, không hiện cửa sổ.
4. Thử Access Bridge bình thường và ép bật.
5. Nhúng runtime, chạy chính EXE với --diagnose.

Trên máy nhận Windows:

~~~powershell
$p = Start-Process .\TankOps.exe -ArgumentList '--diagnose' -PassThru -Wait
$p.ExitCode
~~~

0 là self-test đạt. Chế độ này không cần server. Nó kiểm tra giải mã âm thanh, chưa kiểm chứng loa/driver hoặc độ mượt trận.

Native Mac và native Ubuntu cũng kiểm tra runtime đóng gói trước khi tạo archive cuối. Chưa chạy native Mac/Linux trong môi trường Windows hiện tại; kiểm tra cú pháp không thay thế chạy trên OS đích.

Build Windows còn chạy chính EXE với profile tạm: lần đầu không có Java trong PATH; lần sau có JAVA_HOME không tồn tại và các tùy chọn JVM sai; cuối cùng là EXE/thư mục profile có ký tự Unicode. Cả ba phải trả mã 0 trước khi xuất EXE. Bài kiểm tra sử dụng runtime nhúng và giữ log trong thư mục tạm để đối chiếu; không xóa cache người dùng.

### Maven

Chạy common/server/client bằng JDK 21+:

~~~bash
mvn -B -pl tankops-client,tankops-server -am test
~~~

Kiểm thử sạch sau khi dọn: common 77, server 46, client 22, tổng **145 tests**, 0 failure/error. Bốn test của lớp InterpolationBuffer đã bị thay thế được bỏ cùng lớp cũ; không cộng các báo cáo XML cũ còn nằm trong target.

Các nhóm test cần giữ: engine/map/codec malformed và round-trip; room/lifecycle/concurrency/stress/socket; input nhanh/reset; nội suy; preview va chạm/mất snapshot/respawn; đo RTT. TickRateExperiment là công cụ thí nghiệm của nhóm, vẫn giữ.

Không gộp test database vào số trên: test MongoDB riêng có thể cần dịch vụ ngoài và có thao tác ghi. Chưa coi database transaction hoặc lưu trận end-to-end là đã nghiệm thu.

### Ma trận yêu cầu cũ

| TC | Bằng chứng/trạng thái hiện tại |
| --- | --- |
| TC01–05 | Engine có test đạn, va chạm, tường, chết/respawn; không suy ra mọi trường hợp gameplay đã được phủ |
| TC06–07 | Round-trip và malformed protocol có kiểm thử |
| TC08–10 | Join room, room full và concurrent join có kiểm thử |
| TC11 | Có cleanup/disconnect, stress và socket tests; cần thử thực tế ngắt máy giữa trận |
| TC12 | UI multiplayer đã nối; cần nghiệm thu nhiều máy nhìn thấy diễn biến nhất quán |
| TC13–14 | Repository MongoDB đã có nhưng chưa nối server và chưa chứng minh ACID/rollback cho lưu trận |
| TC15 | Chưa có leaderboard trong luồng game hiện tại |
| TC16 | Chưa triển khai ReplayRecorder/ReplayPlayer |
| TC17 | Có test input/reset và startup; độ mượt/phản hồi qua Internet cần đo thực tế |

### Trước buổi demo

- Chạy trên máy sạch/VM chưa cài Java, đúng OS/chip. Mô phỏng PATH sạch trên máy build chưa thay thế hoàn toàn máy sạch.
- Thử giải nén cache lần đầu bằng tài khoản thử nghiệm; thử tên tài khoản/đường dẫn có dấu và khoảng trắng.
- Mở offline, kiểm tra hình ảnh/phím/âm thanh rồi chơi 2 máy khác mạng; thử READY/START, bắn, chết, kết quả, rời và chơi lại.
- Thử đủ 5 người/phòng và từ chối người thứ 6; đo RTT/tick/render trước khi kết luận tải.
- Ghi SHA-256 của đúng bản đã test, phát đúng file đó. Không lấy kết quả test bản cũ để chứng minh file vừa build lại.

## 9. Chẩn đoán lỗi thường gặp

| Hiện tượng | Kiểm tra/xử lý |
| --- | --- |
| SSH 127.0.0.1:2222 bị từ chối | Chạy lệnh phía Windows; VM đang bật, NAT forwarding đúng máy ảo, sshd nghe cổng 22 |
| Không thấy ~/TankOps | Chưa chuyển ZIP hoặc giải nén; scp chạy từ PowerShell Windows, unzip chạy trong Ubuntu |
| --local nhưng log vẫn hiện IP Tailscale | Script Ubuntu cũ đã bỏ qua tham số; Ctrl+C rồi chạy Java với -Dtankops.bindAddress=127.0.0.1 và kiểm tra log/ss trước khi mở Pinggy |
| Thấy phòng nhưng START không vào trận | Đủ 2 người/READY/host; đối chiếu client/server cùng protocol, đọc log sau START |
| Phòng báo PLAYING nhưng client đứng màn chờ | Không kết luận tunnel hỏng; kiểm tra snapshot/decode và phiên bản hai đầu |
| Build thiếu JDK | Chỉ định -JdkHome tới thư mục JDK 21+ có javac/jar/jlink/jdeps; Java trong Ubuntu không dùng để build Windows |
| PowerShell chặn .ps1 | Dùng powershell -NoProfile -ExecutionPolicy Bypass -File cho lần build; không cần đổi chính sách lâu dài |
| Failed to launch JVM / exit code 1 | Dùng EXE mới và đọc log; không cài Java ngoài theo phỏng đoán |
| ClassNotFoundException AccessBridge | Runtime phải có jdk.accessibility; test-AppImage và --diagnose đã kiểm tra trường hợp bật trợ năng |
| Tên Windows có dấu | Launcher thử đường dẫn ngắn; nếu không có thì cache cạnh EXE trong thư mục ASCII có quyền ghi |
| Không ghi đè được EXE | Đóng game cũ; không tự giết phiên chơi đang chạy để build |
| Xe giật/phím chậm | Xem RTT và cảnh báo snapshot; so với kết nối trực tiếp, đo tick/render. Không tăng tick rate tùy tiện |

Log tại %LOCALAPPDATA%/TankOps/logs hoặc .tankops-cache/logs cạnh EXE khi dùng cache dự phòng. Giữ nguyên thông tin lỗi trước khi xóa cache/cài lại.

## 10. Công việc còn lại

1. Nghiệm thu native Mac Intel/Apple Silicon và Ubuntu bằng gói tự chứa, máy nhận chưa có Java.
2. Đo nhiều mẫu RTT/jitter, tuổi queue, tick và render khi chơi thực tế 2–5 người; chưa hứa mức FPS/latency tuyệt đối.
3. Nếu cần prediction đầy đủ: thiết kế sequence input/ACK, replay và reconciliation; InputMessage.tick hiện chưa phải ACK sequence.
4. Tối ưu encode broadcast một lần và delta map khi có số đo; phải bảo toàn âm thanh/sự kiện và đồng bộ lại khi join.
5. Tích hợp MongoDB với finishGame bất đồng bộ và test lỗi/transaction; cấu hình kết nối database cần tách khỏi source trước khi phát triển luồng thật.
6. Replay, leaderboard và benchmark BotClient/BenchmarkRunner vẫn là việc chưa hoàn thành.
7. Có thể thay chat command READY/START bằng message riêng khi cả client/server cùng cập nhật.

## 11. Dọn tài liệu và code

Các hướng dẫn triển khai, macOS, release validation, phân tích hiệu năng và thiết kế mạng cũ đã hợp nhất vào đây; không giữ các bản MD mâu thuẫn với source. protocol.md giữ vai trò đặc tả wire riêng.

Đã bỏ các phần không có tham chiếu runtime: MockGameState, ScreenManager/HudPanel rỗng, InterpolationBuffer cũ cùng test của nó, RuntimeSmokeTest thủ công đã được thay bằng StartupCheck/Test-AppImage, chương trình Hello World database/src/Main.java và test_client.ps1 gửi text không phù hợp TCP binary.

Giữ module database, test có ý nghĩa, TickRateExperiment, Explosion đang được renderer sử dụng và các script đóng gói có đầu ra khác nhau. Không dọn đệ quy target/dist hoặc xóa các bản build/cached runtime đang dùng trong phiên chơi. File trong target là dữ liệu sinh và đã bị Git bỏ qua.
