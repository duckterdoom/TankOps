# Giao thức TCP binary TankOps

*Cập nhật 29/10/2026 theo MessageCodec và các record trong tankops-common. Kiến trúc/triển khai: [SYSTEM_ANALYSIS.md](../SYSTEM_ANALYSIS.md).*

## 1. Khung và kiểu dữ liệu

~~~text
[length: int32 big-endian][typeId: uint8][payload: length-1 bytes]
~~~

length không tính 4 byte tiền tố, nằm trong 1..1,048,576. Codec đọc đủ frame, từ chối type/direction không hợp lệ hoặc payload bị cắt. TCP là byte stream: không dùng ReadLine/WriteLine để trao đổi với server.

| Ký hiệu | Mã hóa Java |
| --- | --- |
| i32 / i64 | writeInt / writeLong, signed, big-endian |
| f64 | writeDouble, IEEE 754, big-endian |
| bool | writeBoolean, 1 byte |
| str | writeUTF/readUTF: độ dài 2 byte và **modified UTF-8**, không phải UTF-8 thô |
| direction | 1 byte: null=0, UP=1, DOWN=2, LEFT=3, RIGHT=4 |
| list<T> | i32 số phần tử rồi lần lượt T |
| map<str,i32> | i32 số cặp, rồi str key + i32 value |

Dưới đây là **thứ tự trên wire**, có thể khác thứ tự tham số constructor. Không suy ra wire format từ record mà bỏ qua codec.

## 2. ID và payload

| ID | Bản tin / hướng dùng | Payload theo thứ tự |
| --- | --- | --- |
| 0x01 | JoinRoom, C→S | str roomId, str playerName |
| 0x02 | JoinResponse, S→C | bool success, str message, str roomId, str assignedPlayerId, i32 playerNumber |
| 0x03 | LeaveRoom, C→S | str roomId, str playerId |
| 0x04 | RoomListRequest, C→S | str requesterId |
| 0x05 | RoomListResponse, S→C | list<RoomInfo> rooms |
| 0x06 | Input, C→S | str playerId, i32 tick, direction, bool shooting |
| 0x07 | Snapshot, S→C | Xem phần 3 |
| 0x08 | GameOver, S→C | str winnerId, map scores, map kills, map deaths |
| 0x09 | Event, S→C | byte eventType ordinal, str sourceId, str targetId, f64 x, f64 y |
| 0x0A | Chat, hai chiều | str sender, str content |
| 0x0B | Ping, hai chiều | i64 timestamp |
| 0x0C | Pong, hai chiều | i64 timestamp phản hồi nguyên giá trị Ping |
| 0x0D | Disconnect, hai chiều theo hợp đồng codec | str playerId, str reason |
| 0x0E | RoomState, S→C | str roomId, list<str> playerNames, str hostId, list<str> readyPlayerNames |

RoomInfo: str roomId, i32 currentPlayers, i32 maxPlayers, str state. State do server gửi là LOBBY, PLAYING hoặc FINISHED.

EventType ordinal hiện tại: TANK_HIT=0, TANK_DESTROYED=1, BULLET_BLOCKED=2, WALL_DESTROYED=3, POWERUP_SPAWNED=4, POWERUP_COLLECTED=5. Việc codec hỗ trợ bản tin không đồng nghĩa mọi sự kiện đã được gameplay phát ở mọi nhánh.

## 3. Snapshot (0x07)

~~~text
i32 tick
bool gameOver
str winnerId
list<TankSnapshot> tanks
list<BulletSnapshot> bullets
list<PowerUpSnapshot> powerUps
list<EnvironmentSnapshot> environments
list<str> sounds
~~~

| Phần tử | Trường theo thứ tự |
| --- | --- |
| TankSnapshot | str id; f64 x, y; direction; i32 health, maxHealth, starLevel; bool shielded, moving, destroyed; i32 lives, score |
| BulletSnapshot | str id, ownerId; f64 x, y; direction |
| PowerUpSnapshot | str id, kind; f64 x, y |
| EnvironmentSnapshot | str id, kind; f64 x, y |

winnerId/hostId null được codec biểu diễn bằng chuỗi rỗng ở các bản tin tương ứng. Danh sách tường chỉ gồm gạch/thép còn lại; cây/nước/băng lấy từ map chung. sounds là sự kiện của tick, không phát lại mỗi frame render hoặc bỏ tùy tiện khi gộp snapshot.

GameOver encode các map theo thứ tự key đã sắp xếp để round-trip byte ổn định; ý nghĩa dữ liệu vẫn là map.

## 4. Hợp đồng lobby và input

- roomId cụ thể: vào đúng phòng; auto: tìm phòng chờ còn chỗ; new: tạo phòng mới rồi vào.
- Tên và room state được server xác nhận. Không tin playerId do client tự khai trong input; session dùng tên/định danh do server quản lý.
- READY và UNREADY là nội dung ChatMessage. /start chỉ dành cho host; server kiểm tra số người và trạng thái sẵn sàng.
- RoomStateMessage là 0x0E, không phải 0x06; InputMessage dùng 0x06.
- Input.tick là tick snapshot client biết, **không phải sequence input đã được ACK**.
- null direction là thả phím. shooting=true là yêu cầu bắn; server vẫn kiểm tra cooldown/gameplay.
- Ping.timestamp chỉ được echo; client đo RTT bằng cùng đồng hồ monotonic của mình, không so trực tiếp giờ hai máy.

## 5. Luồng và tương thích

1. TCP connect → RoomListRequest/Response.
2. JoinRoom → JoinResponse → RoomState.
3. Thành viên READY → RoomState → host /start.
4. Server phát snapshot; client gửi input và Ping/Pong.
5. GameOver → rời phòng/reset → vào ván mới.

Không có handshake phiên bản protocol tự động. Nếu thay field/thứ tự/ID, cập nhật server và mọi client cùng lúc. Có constructor Java cũ không có nghĩa frame cũ được hỗ trợ trên wire. Lobby hoạt động không chứng minh snapshot trận đấu tương thích.

MessageWriter có queue hữu hạn theo kết nối, giữ thứ tự. Không được bỏ các bản tin điều khiển hoặc sounds để tối ưu mà không thiết kế lại hợp đồng. Các kiểm thử codec/round-trip, malformed, socket end-to-end và RoomState nằm trong common/server/client.
