# BÁO CÁO PHÂN TÍCH HIỆN TRẠNG & KẾ HOẠCH HÀNH ĐỘNG HỆ THỐNG TANKOPS
**Đối chiếu giữa Tài liệu Thiết kế Kiến trúc (Google Docs) và Hiện trạng Mã nguồn (Source Code)**

*Cập nhật lần cuối: 26/10/2026*  
*Dự án: TankOps - Multiplayer Real-time Tank Battle*  
*Môi trường chuẩn: Java 21 (LTS), Maven Multi-module, Swing GUI, TCP Binary Protocol, SQLite JDBC*

---

## 1. TỔNG QUAN HỆ THỐNG & ĐÁNH GIÁ TIẾN ĐỘ CHUNG

Theo tài liệu thiết kế, TankOps là hệ thống game bắn xe tăng nhiều người chơi thời gian thực theo kiến trúc **Server-Authoritative**. Hệ thống chia thành 4 module độc lập trong Maven:
- `tankops-common`: Domain model, game engine, bản đồ và giao thức mạng nhị phân chung.
- `tankops-server`: Server TCP, quản lý phiên kết nối, phòng chơi, vòng tick 30 Hz, database và replay.
- `tankops-client`: Client đồ họa Swing, rendering nhiều lớp, xử lý phím bấm, bộ đệm nội suy mượt mà.
- `tankops-tools`: Client bot không đầu (headless) và công cụ đo tải (benchmark).

### Bảng tóm tắt tiến độ các module

| Module / Thành phần | Mức độ hoàn thành | Đánh giá hiện trạng                                                                                                                                                                                                                                                                                                           |
| :--- | :---: |:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`tankops-common`** | **~90%** | Đã hoàn thành hệ thống Model OOP/SOLID/Strategy, nạp Map động, GameEngine đã tích hợp trọn vẹn gameplay logic. **Đã tổ chức phân chia trọn vẹn package `common.net` thành các nhóm package con chuyên biệt (`codec/`, `lobby/`, `game/`, `system/`)**, vượt qua 100% TC01 và TC07.                                            |
| **`tankops-server`** | **~70%** | Đã hoàn thành kiến trúc đa luồng (Acceptor, 1 thread/ClientSession, TickLoop 30 tick/s, RoomManager, GameRoom). **Đã tích hợp MessageCodec vào ClientSession và GameRoom** (phát snapshot nhị phân 30 Hz, nhận Input, Ping/Pong, RoomList, Chat), 23/23 tests passed. Chưa có Database (JDBC) và Replay.                      |
| **`tankops-client`** | **~80%** | Đã dựng đầy đủ bộ giao diện Swing (CardLayout với 5 Panel), Renderer đa lớp, SpriteManager, chế độ Singleplayer offline. **Đã hoàn thiện NetworkClient socket TCP (singleton, thread-safe, receiver thread), MessageListener, GameStateStore, InterpolationBuffer**, 10/10 tests passed. Chưa nối phòng động trên LobbyPanel. |
| **`tankops-tools`** | **0%** | Mới có file `pom.xml`, chưa có mã nguồn BotClient và BenchmarkRunner.                                                                                                                                                                                                                                                         |
| **Docs & Protocol** | **100%** | **Đã hoàn thành `docs/protocol.md`** (đặc tả binary format) và **`docs/NETWORK_DESIGN.md`** (sơ đồ Mermaid + pseudo-code toàn diện). Thư mục `database/` và `deploy/` chưa khởi tạo.                                                                                                                                          |

---

## 2. HIỆN TRẠNG CHI TIẾT TỪNG MODULE

### 2.1. Module `tankops-common`

#### A. Những gì ĐÃ HOÀN THÀNH
1. **Mô hình Hướng đối tượng (OOP & SOLID)**:
   - **`model.common`**:
     - `Entity`: Lớp cơ sở trừu tượng có `id`, tọa độ `Vector2D position`, `width/height`, thuật toán va chạm AABB `intersects(Entity)`.
     - `Vector2D`: Record biểu diễn vector 2 chiều với các hàm tiện ích (`add`, `multiply`).
     - `Direction`: Enum hướng (`UP`, `DOWN`, `LEFT`, `RIGHT`) chuyển đổi vector dịch chuyển `dx, dy`.
     - `Bullet`: Đại diện đạn bay với vận tốc `Bullet.SPEED_SLOW` / `SPEED_FAST`, cấp độ xuyên phá `starLevel` và người bắn `ownerId`.
   - **`model.tank`**:
     - `Tank`: Lớp cha trừu tượng quản lý tốc độ, máu (`health`, `maxHealth`), di chuyển `move()`, bắn `fire()`, và cơ chế trượt băng `slideForward(deltaTime)`.
     - `PlayerTank`: Hỗ trợ nâng cấp sao (`starLevel`), số mạng (`lives`), khiên vô địch (`shielded`), đếm điểm và tự cộng mạng.
     - `EnemyTank`: Lớp cha xe địch, phân nhánh thành 4 lớp cụ thể: `BasicTank`, `FastTank`, `PowerTank`, `ArmorTank`.
     - `state.BotState`: Đã áp dụng triệt để State Pattern cho AI Bot qua các lớp Singleton: `PatrolState`, `ChaseState`, `AttackState`.
   - **`model.environment`**:
     - `Environment`: Lớp cha với hợp đồng trừu tượng `canTankPass()`, `canBulletPass()`, `takeHit(starLevel)`.
     - Các lớp con đa hình: `BrickWall` (bị phá hủy bởi đạn), `SteelWall` (chỉ đạn cấp sao cao mới xuyên được), `Water` (xe không đi qua được, đạn bay qua được), `Trees` (xe và đạn đi qua được), `Ice` (gây hiệu ứng trượt).
   - **`model.powerups`**:
     - `PowerUp`: Lớp cha đóng gói mẫu Strategy qua hàm `applyEffect(PlayerTank)`.
     - Đã triển khai đủ 6 lớp vật phẩm: `Star`, `Helmet`, `Grenade`, `Shovel`, `Timer`, `TankPowerUp`.
2. **Hệ thống Bản đồ (`common.map`)**:
   - `GameMap`: Lưu trữ đối tượng môi trường, điểm spawn người chơi (`playerSpawnPoints`) và địch (`enemySpawnPoints`) và vật phẩm (`powerUpSpawnPoints`).
   - `MapLoader`: Đọc và phân tích file bản đồ text dạng ma trận (`map_single.map`, `map_multi.map`).
3. **Bộ máy trò chơi (`common.engine`)**:
   - `GameState`: Nơi lưu trữ danh sách xe tăng, đạn, địa hình, vật phẩm và trạng thái kết thúc trận `isGameOver`.
   - `GameEngine`: Nạp môi trường, `spawnPlayers()`, `spawnBots()`, nhận di chuyển `handleMoveEvent()`, kiểm tra va chạm tường và rollback (TC02), tích hợp thành công cơ chế quán tính trượt băng (Ice Slide) mượt mà cho cả khi giữ phím và thả phím. Đã hoàn thiện logic bắn đạn `handleShootEvent()`, tính toán tọa độ đạn bay `updateBullets()`, xử lý va chạm chính xác giữa Đạn - Tường (trừ độ bền môi trường TC03, TC04) và Đạn - Xe Tăng (trừ máu/mạng TC01). Đã có `onKillCallback()` để báo sự kiện hạ gục về cho Server tính điểm. Tự động kiểm tra điều kiện kết thúc trận `checkGameOverCondition()`.Tích hợp Bot logic tính toán khoảng cách tự động để đổi trạng thái Bot `updateBots()`.Tích hợp bộ đếm thời gian `powerUpSpawnTimer` và khoảng thời gian sinh vật phẩm định kỳ (`SPAWN_INTERVAL_TICKS`). Sinh ngẫu nhiên 6 loại vật phẩm (Grenade, Helmet, Shovel, Star, TankPowerUp, Timer) tại các điểm spawn của bản đồ. Tích hợp phương thức `checkPowerUpCollisions()` xử lý va chạm và kích hoạt hiệu ứng qua Strategy Pattern (`powerUp.applyEffect(playerTank, this.gameState)`).
4. **Giao thức mạng hoàn chỉnh (`common.net`) - Phân chia theo Package khoa học**:
   - **`tankops.common.net.Message`**: Hợp đồng gốc chuẩn cho toàn bộ bản tin mạng (`byte getTypeId()`).
   - **`tankops.common.net.codec`**:
     - `MessageCodec`: Bộ mã hóa/giải mã nhị phân có tiền tố độ dài `[length: 4 bytes Big-Endian][typeId: 1 byte][payload]`. Có cơ chế kiểm soát `MAX_MESSAGE_SIZE = 1MB` phòng vệ DoS.
     - `ProtocolException`: Ngoại lệ ném ra khi gặp gói tin sai định dạng hoặc dữ liệu rác.
   - **`tankops.common.net.lobby`**:
     - `JoinRoomMessage` (`0x01`), `JoinResponseMessage` (`0x02`), `LeaveRoomMessage` (`0x03`).
     - `RoomListRequestMessage` (`0x04`), `RoomListResponseMessage` (`0x05`), `RoomInfo` record.
   - **`tankops.common.net.game`**:
     - `InputMessage` (`0x06`) truyền phím bấm, hướng `Direction` (hỗ trợ `null` khi đứng yên), cờ bắn `shooting`.
     - `SnapshotMessage` (`0x07`) truyền trạng thái xe `TankSnapshot`, đạn `BulletSnapshot`, cờ game over, winner.
     - `GameOverMessage` (`0x08`) truyền `winnerId` và bảng điểm tổng kết.
     - `EventMessage` (`0x09`) truyền sự kiện rời rạc (nổ xe, trúng đạn, nhặt đồ).
   - **`tankops.common.net.system`**:
     - `ChatMessage` (`0x0A`) chat trong phòng.
     - `PingMessage` (`0x0B`) & `PongMessage` (`0x0C`) đo RTT latency và duy trì kết nối (Heartbeat).
     - `DisconnectMessage` (`0x0D`) ngắt kết nối an toàn kèm lý do.
   - **Kiểm thử `common.net` (47/47 tests passed)**:
     - `MessageCodecTest`: 30 tests (Round-Trip toàn diện TC06, Malformed Message Rejection TC07, Stream I/O).
     - `LobbyMessageRoundTripTest`: 5 tests chuyên sâu cho nhóm sảnh/phòng.
     - `GameMessageRoundTripTest`: 5 tests chuyên sâu cho nhóm gameplay.

---

### 2.2. Module `tankops-server`

#### A. Những gì ĐÃ HOÀN THÀNH
1. **Kiến trúc mạng & Đa luồng (Server Concurrency)**:
   - `GameServer`: Mở `ServerSocket`, đọc `config.properties`, quản lý danh sách `ClientSession` an toàn với `synchronized`.
   - `ConnectionAcceptor`: Luồng riêng chấp nhận kết nối TCP từ client và chuyển giao cho `ClientSession`.
   - `ClientSession`: Mỗi client kết nối chạy trên một `Thread` độc lập, cấu hình `socket.setTcpNoDelay(true)`.
     - Tích hợp `MessageCodec.readMessage(in)` để đọc các Message nhị phân, gửi qua `send(Message)` đồng bộ thread-safe.
     - Tự động điều phối: gia nhập phòng (`JoinRoomMessage` $\rightarrow$ `JoinResponseMessage`), rời phòng (`LeaveRoomMessage`), lấy danh sách phòng (`RoomListRequestMessage` $\rightarrow$ `RoomListResponseMessage`), cập nhật lệnh di chuyển (`InputMessage`), phản hồi Ping/Pong và Chat.
     - Đảm bảo tương thích hoàn toàn với session giả lập `ClientSession(null, null)` trong unit tests.
2. **Quản lý phòng & Vòng lặp trận đấu**:
   - `RoomManager`: Tạo phòng, hủy phòng, cho client vào/rời phòng, hỗ trợ `getRoom(roomId)`, khống chế số lượng người chơi tối đa (`maxPlayers = 5`) và số phòng (`maxRooms`).
   - `GameRoom`:
     - Quản lý trạng thái phòng bằng Enum `RoomState` (`LOBBY` $\rightarrow$ `PLAYING` $\rightarrow$ `FINISHED`).
     - Áp dụng nguyên tắc **Single-Writer**: Luồng mạng client chỉ nạp lệnh vào `ConcurrentLinkedQueue<MoveCommand>`. Chỉ duy nhất luồng `TickLoop` được phép gọi cập nhật `GameEngine`.
     - `TickLoop`: Vòng lặp tính toán cố định 30 tick/giây (`ScheduledExecutorService`).
     - **Tích hợp Snapshot nhị phân**: Tại mỗi tick gọi `buildBinarySnapshot()` và `broadcast(SnapshotMessage)` gửi tới mọi người chơi; kết thúc trận broadcast `GameOverMessage`.
     - Tính điểm (`scores`, `addKill`), kiểm tra kết thúc trận (`isGameOver`) và tìm người thắng (`findWinner`).
3. **Kiểm thử tự động Concurrency (23/23 tests passed)**:
   - `RoomLifecycleTest`: Vòng đời phòng LOBBY $\rightarrow$ PLAYING $\rightarrow$ FINISHED.
   - `RoomManagerTest`: Đạt **TC09** (từ chối người thứ 6 khi phòng 5 chỗ đã đầy) và **TC10** (50 thread đồng thời vào phòng 1 chỗ, bảo đảm chỉ đúng 1 người thành công).
   - `GameRoomTickTest`: Kiểm tra tick rate ổn định và chịu bão lệnh từ nhiều thread.
   - `ServerStressTest`: Chạy 20 luồng giả lập tạo, vào, thoát phòng liên tục không bị deadlock; stress test kết nối/ngắt socket dọn sạch session.

---

### 2.3. Module `tankops-client`

#### A. Những gì ĐÃ HOÀN THÀNH
1. **Giao diện người dùng (Swing GUI)**:
   - `MainFrame`: Kích thước $1024 \times 768$, chuyển cảnh mượt mà bằng `CardLayout`.
   - Đầy đủ 5 màn hình: `LoginPanel`, `ModeSelectPanel`, `LobbyPanel`, `GamePanel`, `ResultPanel`, `HudPanel`.
2. **Đồ họa & Rendering (`ui.render`)**:
   - `SpriteManager`: Tự động cắt các khung hình sprite sheet Battle City gốc.
   - `GameRenderer`: Vẽ đồ họa đa tầng (Băng, Nước hoạt ảnh 2 frame, Gạch, Thép, Xe tăng kèm thanh máu, Đạn, Cây cối che phủ).
3. **Chơi đơn Offline (Singleplayer)**:
   - `SinglePlayerController` & `InputHandler`: Chơi offline mượt mà, di chuyển xe né tường (TC17).
4. **Mạng & Trạng thái Client (`client.net`, `client.state`)**:
   - **`NetworkClient`**: Singleton, thread-safe, kết nối TCP Socket thực tế với timeout 5s, `setTcpNoDelay(true)`, chạy luồng daemon `NetworkClient-Receiver` liên tục nhận và giải mã Message, phương thức `sendMessage(Message)` an toàn đa luồng, hỗ trợ Ping/Pong tự động.
   - **`MessageListener`**: Interface nhận sự kiện bản tin đến, kết nối (`onConnected`) và mất kết nối (`onDisconnected`).
   - **`GameStateStore`**: Model MVC lắng nghe `SnapshotMessage`, `GameOverMessage`, `JoinResponseMessage` từ server để cập nhật trạng thái hiển thị an toàn bằng `AtomicReference`.
   - **`InterpolationBuffer`**: Bộ đệm nội suy tuyến tính (LERP) làm mượt vị trí xe từ xa giữa 2 snapshot liên tiếp, loại bỏ hiện tượng giật giật (jitter).
5. **Kiểm thử tự động Client (10/10 tests passed)**:
   - `NetworkClientTest`: Kiểm thử kết nối TCP thật, gửi `InputMessage` và nhận `JoinResponseMessage` qua loopback socket (TC08).
   - `GameStateStoreTest`: Kiểm thử chuyển đổi trạng thái khi nhận bản tin mạng.
   - `InterpolationBufferTest`: Kiểm thử công thức LERP, độ mượt nội suy và giới hạn dung lượng hàng đợi snapshot.

---

## 3. MA TRẬN ĐỐI CHIẾU TEST CASES (THEO MỤC 11 GOOGLE DOCS)

| Mã TC | Tên Test Case | Mô tả kiểm thử | Hiện trạng trong Code                                                               | Trạng thái |
| :---: | :--- | :--- |:------------------------------------------------------------------------------------| :---: |
| **TC01** | Bullet hits tank | Đạn trúng xe: Xe mất máu, đạn biến mất | Đã có trong `GameEngineTest` (`testTC01_BulletHitsEnemy`)                           | 🟢 Đạt |
| **TC02** | Tank hits wall | Xe đụng tường cản: Xe dừng lại, rollback vị trí | Đã có trong `GameEngineTest` (`testTC02_TankCollidesWithWallAndRollsBack`)          | 🟢 Đạt |
| **TC03** | Bullet destroys brick wall | Đạn phá tường gạch: Tường bị vỡ/biến mất | Đã có trong `GameEngineTest` (`testTC03_BulletCollidesWithObstacleWallAndRemoves`)  | 🟢 Đạt |
| **TC04** | Bullet hits steel wall | Đạn đụng tường thép: Tường giữ nguyên, đạn vỡ | Đã có trong `GameEngineTest` (`testTC04_BulletPassesThroughPassableEnvironment`)  | 🟢 Đạt |
| **TC05** | Tank death & respawn | Máu = 0: Tính mạng/kill, hồi sinh sau thời gian trễ | CĐã có trong `GameEngineTest` (`testTC05_PlayerTankRespawnsWhenLivesRemain `) | 🟢 Đạt |
| **TC06** | Protocol round trip | Mã hóa rồi giải mã các loại Message: Giữ nguyên dữ liệu | **Đã hoàn thành 100% trong `MessageCodecTest` & các sub-suites (30 tests)**         | 🟢 Đạt |
| **TC07** | Malformed message | Gửi byte rác/bị lỗi: Server từ chối không crash | **Đã hoàn thành 100% trong `MessageCodecTest` (8 tests fuzzing/chống DoS)**         | 🟢 Đạt |
| **TC08** | Join room | Gửi JOIN_ROOM: Xuất hiện trong danh sách phòng | **Đã hoàn thành trong `NetworkClientTest` & `ClientSession` qua Socket thật**       | 🟢 Đạt |
| **TC09** | Room full | Người thứ 6 vào phòng 5 chỗ: Báo lỗi từ chối | Đã có trong `RoomManagerTest` (`tc09_roomFull_sixthPlayerRefused`)                  | 🟢 Đạt |
| **TC10** | Concurrent join race | 50 người cùng tranh 1 chỗ: Duy nhất 1 người thành công | Đã có trong `RoomManagerTest` (`tc10_concurrentJoin_exactlyOneSucceeds`)            | 🟢 Đạt |
| **TC11** | Disconnect mid-game | Giết tiến trình client: Trận đấu vẫn tiếp diễn, xóa xe client | Đã có dọn dẹp an toàn ở `ClientSession.cleanup`                                     | 🟡 Cần viết integration test |
| **TC12** | Client consistency | Nhiều client cùng chơi nhìn thấy một trạng thái thế giới | Chờ nối giao diện Multiplayer UI                                                    | 🔴 Chưa đạt |
| **TC13** | Save match result | Trận đấu xong: Lưu các bản ghi match & players trong 1 transaction | Chờ Database module (`MatchRepository`)                                             | 🔴 Chưa đạt |
| **TC14** | Transaction rollback | Cố tình lỗi khi lưu kết quả: Không lưu dữ liệu rác | Chờ Database module (`MatchRepository`)                                             | 🔴 Chưa đạt |
| **TC15** | Leaderboard query | Đọc top N người chơi theo điểm | Chờ Database module (`PlayerRepository`)                                            | 🔴 Chưa đạt |
| **TC16** | Replay playback | Ghi trận đấu ra file nhị phân và đọc lại phát lại | Chờ Replay module (`ReplayRecorder`/`Player`)                                       | 🔴 Chưa đạt |
| **TC17** | GUI key input | Nhấn phím WASD/bắn: Xe phản hồi ngay, không giật lag | Đã test thực tế trên Singleplayer GUI                                               | 🟢 Đạt |

---

## 4. TỔNG KẾT KẾT QUẢ BUILD & TEST TOÀN DỰ ÁN

```
[INFO] Reactor Summary for tankops-parent 1.0-SNAPSHOT:
[INFO] tankops-parent ..................................... SUCCESS [  0.002 s]
[INFO] tankops-common ..................................... SUCCESS [  1.382 s]  (47/47 tests passed)
[INFO] tankops-server ..................................... SUCCESS [  5.509 s]  (23/23 tests passed)
[INFO] tankops-client ..................................... SUCCESS [  1.335 s]  (10/10 tests passed)
[INFO] tankops-tools ...................................... SUCCESS [  0.013 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS (Tổng cộng 80/80 tests PASSED, 0 FAILURES, 0 ERRORS)
```

---

## 5. PHƯƠNG ÁN & KẾ HOẠCH HÀNH ĐỘNG TIẾP THEO CHO CÁC ROLE

Dưới đây là kế hoạch chi tiết, rõ ràng về mặt kiến trúc và phân công kỹ thuật cho các thành viên trong nhóm theo từng vai trò:

### 🎮 Role 1: Server Developer (`tankops-server`)
**Trách nhiệm chính:** Vòng lặp game (TickLoop), quản lý phòng, phiên client, xử lý đồng thời.

* **Nhiệm vụ 1: Nối lệnh Bắn (`shooting`) từ InputMessage vào GameRoom**:
  - Hiện tại `ClientSession` mới chỉ chuyển hướng di chuyển: `room.submitMove(playerId, input.direction())`.
  - *Cần làm:* Thêm hàng đợi lệnh bắn hoặc mở rộng `MoveCommand` thành `PlayerInputCommand(String playerId, Direction dir, boolean shoot)`.
  - Trong nhịp `tick()` của `GameRoom`, nếu `cmd.shoot() == true`, gọi `engine.handleShootEvent(cmd.playerId())`.
* **Nhiệm vụ 2: Cơ chế phòng chơi hoàn chỉnh**:
  - Hỗ trợ chủ phòng bấm "Bắt đầu trận đấu" (`StartGameMessage`), kiểm tra số lượng $\ge 2$ người chơi hoặc tự động chèn bot.
  - Tự động nạp map được chọn (`map_multi.map`) thay vì hardcode.
  - Dọn dẹp phòng sạch sẽ khi trận đấu kết thúc hoặc người chơi cuối cùng rời đi (tránh rò rỉ bộ nhớ/thread).
* **Nhiệm vụ 3: Tích hợp bất đồng bộ với Database & Replay**:
  - Khi trận kết thúc (`GameRoom.finishGame()`), tạo một tác vụ nền (`CompletableFuture`) gửi bảng điểm sang `MatchRepository` để lưu vào SQLite mà không làm giật lag vòng tick của các phòng khác.

---

### ⚙️ Role 2: Game Engine Developer (`tankops-common/engine`, `common/model`)
**Trách nhiệm chính:** Thực thể, vật lý, phát hiện va chạm, AI bots, powerups.


---

### 🖥️ Role 3: Client Developer (`tankops-client`)
**Trách nhiệm chính:** Giao diện Swing GUI, rendering nhiều lớp, xử lý phím bấm, đồng bộ trạng thái.

* **Nhiệm vụ 1: Nối mạng động cho màn hình Sảnh (`LobbyPanel`)**:
  - `LobbyPanel` đăng ký làm `MessageListener` với `NetworkClient`.
  - Khi chuyển vào màn hình sảnh: Gửi `RoomListRequestMessage`, nhận `RoomListResponseMessage` để hiển thị danh sách phòng thật từ Server lên JTable / JList.
  - Thêm nút "Tạo phòng" (gửi `JoinRoomMessage` với `roomId="auto"` hoặc tên mới).
  - Khi nhận `JoinResponseMessage(success=true)`: Hiển thị danh sách người chơi trong phòng và nút "Sẵn sàng / Bắt đầu".
* **Nhiệm vụ 2: Nối Game Loop Multiplayer trong `GamePanel`**:
  - Khi trận đấu bắt đầu: Chuyển màn hình sang `GamePanel` ở chế độ Multiplayer.
  - Vòng lặp render của Client (Swing Timer 60 FPS) đọc dữ liệu xe và đạn từ `GameStateStore.getLatestSnapshot()` và vị trí mịn từ `InterpolationBuffer`.
  - `InputHandler` khi người chơi nhấn phím WASD / J: Đóng gói thành `InputMessage` và gọi `NetworkClient.getInstance().sendMessage(msg)` gửi về Server (chế độ Server-Authoritative, client không tự ý cập nhật vị trí).
* **Nhiệm vụ 3: Màn hình Kết quả (`ResultPanel`)**:
  - Lắng nghe `GameOverMessage` từ `GameStateStore`: Dừng nhận phím, chuyển cảnh sang `ResultPanel` hiển thị Winner và danh sách điểm số nhận được từ Server.
* **Nhiệm vụ 4: Hệ thống Âm thanh (Audio Clips)**:
  - Tạo `SoundManager` phát hiệu ứng âm thanh bắn, nổ xe, nhặt đồ khi nhận `EventMessage`.

---

### 🗄️ Role 4: Database, Testing & Tools Developer (`persistence`, `tools`)
**Trách nhiệm chính:** SQLite JDBC, Replay System, Benchmarking, CI/CD và Build Automation.

* **Nhiệm vụ 1: Tích hợp Cơ sở dữ liệu SQLite (`tankops.server.persistence`)**:
  - Viết file `database/schema.sql` gồm 3 bảng:
    + `players` (`id`, `username`, `total_score`, `created_at`).
    + `matches` (`id`, `start_time`, `end_time`, `winner_id`).
    + `match_players` (`match_id`, `player_id`, `score`, `kills`).
  - Triển khai `DatabaseConnection` quản lý kết nối SQLite qua JDBC (`tankops.db`).
  - Triển khai `MatchRepository.saveMatchResult()` với **ACID Transaction** (`setAutoCommit(false)`, `commit()`, `rollback()` khi lỗi - đạt **TC13, TC14**).
  - Triển khai `PlayerRepository.getLeaderboard(int topN)` đọc bảng xếp hạng (đạt **TC15**).
* **Nhiệm vụ 2: Hệ thống Ghi & Phát lại Trận đấu (`Replay System`)**:
  - `ReplayRecorder`: Lắng nghe từng `SnapshotMessage` từ `GameRoom`, ghi tuần tự ra file nhị phân `.rec` bằng `DataOutputStream` (đóng gói Binary I/O).
  - `ReplayPlayer`: Đọc file `.rec`, phát lại từng snapshot theo đúng nhịp thời gian vào `GameStateStore` để `GameRenderer` hiển thị lại trận đấu (đạt **TC16**).
* **Nhiệm vụ 3: Xây dựng Công cụ Đo tải (`tankops-tools`)**:
  - `BotClient`: Client không đầu (headless), kết nối TCP tới server qua `NetworkClient`, tự động gửi `InputMessage` ngẫu nhiên.
  - `BenchmarkRunner`: Khởi chạy 50 - 100 `BotClient` đồng thời để đo:
    + CPU & RAM usage của Server.
    + RTT Latency khi số lượng client tăng cao (phục vụ Thí nghiệm E1 trong tài liệu).
* **Nhiệm vụ 4: Đóng gói & Triển khai (Deployment)**:
  - Viết `Dockerfile` nhiều tầng (multi-stage build) để đóng gói `tankops-server` thành container gọn nhẹ.
  - Viết script `run-server.sh` / `run-client.bat` phục vụ chạy thử nghiệm nghiệm thu nhanh.
