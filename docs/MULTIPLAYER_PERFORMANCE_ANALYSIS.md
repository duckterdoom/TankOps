# Phân tích độ mượt multiplayer

## Bổ sung phản hồi cục bộ và đo trong trận

- Client có `LocalTankPreview`: phản hồi hiển thị theo phím ở nhịp render, giới hạn lệch tối đa 12 px so với snapshot mới nhất. Dùng tốc độ mặc định của player ở server 30 Hz; đây là dự đoán hình ảnh có giới hạn, không phải replay input có ACK và không sửa GameState authoritative.
- Kiểm tra AABB với tường/nước và biên map; bỏ dự đoán trên băng, khi chết, respawn hoặc đổi ván. Ngừng tiến thêm khi không nhận snapshot mới quá 250 ms. Server vẫn quyết định vị trí thật, đạn, sát thương, điểm; có thể thấy điều chỉnh vị trí nhỏ khi đồng bộ.
- Có RTT ứng dụng ở góc màn chơi, đo bằng Ping/Pong có sẵn mỗi giây; cảnh báo khi snapshot không cập nhật quá 250 ms. RTT gồm chờ writer và xử lý ứng dụng, không chỉ đường truyền mạng. Không thay định dạng giao thức, server hiện tại vẫn nhận được.
- Nhả trạng thái phím khi cửa sổ mất focus hoặc dừng ván, tránh xe tiếp tục chạy/bắn vì sự kiện release không tới game.
- Âm thanh có queue hữu hạn, giới hạn âm trượt băng lặp lại và đóng stream sau khi nạp clip. Đây là giới hạn tác vụ âm thanh, không bỏ sự kiện gameplay hoặc snapshot.
- Test mới kiểm tra phản hồi trước snapshot tiếp theo, giới hạn dự đoán khi mạng ngừng cập nhật, tường/nước/biên, respawn/băng/chết, RTT qua TCP và reset phím. Chưa đo cải thiện thực tế trên máy người chơi qua Pinggy.

Các nhận xét phía dưới về việc chưa có dự đoán mô tả baseline trước thay đổi này. Chưa có prediction/reconciliation đầy đủ với sequence ACK và chưa thay đổi tick rate.

## Cập nhật triển khai

Các mục phân tích bên dưới mô tả trạng thái trước tối ưu. Hiện đã thực hiện:

- Writer nền có hàng đợi tối đa 32 bản tin cho mỗi kết nối, ở cả client/server. Giữ thứ tự và không bỏ snapshot chứa âm thanh; khi queue đầy đóng kết nối thay vì chặn EDT/tick hoặc tích lũy vô hạn. Không đảm bảo chất lượng chơi cho client có đường truyền không theo kịp; client đó có thể bị ngắt để bảo vệ phòng.
- Gửi ngay thay đổi input, kể cả nhấn/thả bắn trong cùng một chu kỳ Timer. Giữ nhịp bắn khi giữ phím khoảng 30 Hz; trạng thái không đổi gửi lại khoảng mỗi 100 ms, giảm gói trùng.
- Render multiplayer dùng Timer 16 ms độc lập với snapshot, nội suy xe từ xa/đạn với đệm 66 ms và đồng hồ đơn điệu. Xe mình vẫn dùng vị trí authoritative mới nhất, không cộng đệm nội suy. Chưa triển khai prediction/reconciliation nên vẫn còn thời gian chờ server phản hồi.
- Không nội suy qua respawn, đổi hướng hoặc dịch chuyển lớn; không extrapolate vô hạn khi mất snapshot. Cache map được cập nhật khi dữ liệu thay đổi, tránh dựng lại mọi frame.
- Không dựng lại danh sách phòng/người chơi khi các panel này đang bị ẩn. Khởi tạo multiplayer có tính idempotent để tránh khởi động controller hai lần khi chuyển màn hình.
- Ổn định thứ tự encode bảng điểm/kill/death để loại lỗi round-trip ngẫu nhiên phát hiện trong lúc test. Giữ nguyên cấu trúc giao thức và tick rate/gameplay.

Kiểm chứng: Maven common/server/client thành công với 142 tests, gồm writer bị chặn/queue đầy, thứ tự bản tin, input nhấn-thả nhanh qua TCP thật, nội suy bằng clock giả lập và bộ test server/lifecycle có sẵn. Chưa có số đo RTT hoặc so sánh độ mượt qua Pinggy thực tế. Các đề xuất đo lường, prediction và delta map bên dưới vẫn là công việc tiếp theo.

Ngày: 28/10/2026. Triệu chứng: xe di chuyển giật và phản hồi phím chậm khi chơi khác mạng qua Pinggy.

## Phạm vi và kết luận

Phân tích mã nguồn hiện tại; server Ubuntu của người dùng đang tắt. Chưa đo RTT, jitter, tải CPU hoặc thời gian ghi socket trên đường truyền thực. Không kết luận Pinggy là nguyên nhân chính. Chưa sửa gameplay, giao thức, EXE hoặc ZIP trong đợt phân tích này.

Có các điểm trong code có thể làm tăng giật và độ trễ, độc lập với nhà cung cấp mạng. Ưu tiên đo rồi xử lý việc ghi mạng trên luồng giao diện/tick và cách render snapshot trước khi thêm dự đoán chuyển động.

## Bằng chứng từ code

| Phát hiện | Vị trí | Hệ quả và giới hạn kết luận |
| --- | --- | --- |
| `MultiplayerController.start()` dùng Swing Timer 16 ms để gọi `NetworkClient.sendMessage()` | `tankops-client/.../controller/MultiplayerController.java` | Gửi input trên EDT, chung luồng với bàn phím và vẽ giao diện. Nếu chờ khóa output hoặc ghi mạng bị chặn thì giao diện và lần lấy input tiếp theo cũng bị chậm. Chưa đo mức chặn thực tế. |
| `sendMessage()` ghi đồng bộ; codec gọi `out.flush()` cho từng message | `tankops-client/.../net/NetworkClient.java`, `tankops-common/.../net/codec/MessageCodec.java` | Không có writer nền tách khỏi EDT. `TCP_NODELAY` đã bật ở hai đầu, nên bật lại tùy chọn này không giải quyết điểm trên. |
| `GameRoom.tick()` gọi `broadcast()`; broadcast lần lượt gọi `ClientSession.send()` và ghi socket đồng bộ | `tankops-server/.../game/GameRoom.java`, `tankops-server/.../net/ClientSession.java` | Một client nhận chậm có thể làm chậm tick của cả phòng khi bộ đệm mạng đầy hoặc chờ khóa output. Có nguy cơ, chưa chứng minh đây là nút thắt trong phiên chơi vừa qua. |
| Multiplayer vẽ trực tiếp `getTanks()`/`getBullets()` mới nhất; repaint theo thông báo snapshot | `tankops-client/.../ui/panels/GamePanel.java`, `state/GameStateStore.java` | Chưa có vòng vẽ multiplayer độc lập 60 FPS. Thời gian gói đến không đều có thể hiện thành bước nhảy vị trí. Server mặc định 30 Hz. |
| `InterpolationBuffer` có sẵn nhưng chưa nối vào đường render multiplayer | `tankops-client/.../state/InterpolationBuffer.java` | Chưa dùng nội suy để che dao động thời gian nhận snapshot. Bộ đệm hiện dùng thời điểm nhận, chưa có đồng bộ thời gian/tick server. |
| Dựng lại tường và vật phẩm trong `paintComponent()` | `tankops-client/.../ui/panels/GamePanel.java` | Cấp phát và xử lý map mỗi lần vẽ dù dữ liệu chưa đổi; có thể tăng công việc trên EDT. Cần profiling để biết mức ảnh hưởng. |
| Snapshot gửi lại toàn bộ tường gạch/thép còn lại mỗi tick và mã hóa riêng khi gửi cho từng client | `tankops-server/.../game/GameRoom.java`, `net/ClientSession.java` | Có dư địa giảm lưu lượng và CPU. Chưa đo bytes/s nên chưa kết luận nghẽn băng thông. |
| Di chuyển chỉ xuất hiện sau snapshot xác nhận từ server | `controller/MultiplayerController.java`, `state/GameStateStore.java` | Chưa có local prediction/reconciliation. Người chơi cảm nhận thời gian input đi đến server cộng thời gian snapshot quay lại. |
| Queue input chưa giới hạn, client gửi cả trạng thái không đổi mỗi 16 ms | `GameRoom.inputQueue`, `MultiplayerController.start()` | Có thể tích lũy lệnh khi server nghẽn hoặc nhận theo cụm. Engine giữ hướng di chuyển cuối, không di chuyển một bước cho mỗi gói input. |

## Ngân sách độ trễ hiện tại

Từ lúc nhấn phím tới hình ảnh cập nhật gồm:

1. Đợi Swing Timer lấy input: danh nghĩa 0–16 ms nếu EDT không bận.
2. Ghi mạng và truyền input từ client qua đường hầm tới server.
3. Đợi tick: danh nghĩa 0–33,3 ms với server 30 Hz nếu tick không bị chậm.
4. Tính toán, broadcast, truyền snapshot về client và giải mã.
5. Đợi EDT vẽ lại.

Các khoảng chờ thực có thể lớn hơn khi EDT, socket hoặc server bị chặn. Đây là phân tích cấu trúc, không phải số đo của phiên chơi. Nội suy giúp chuyển động mượt hơn nhưng thêm thời gian đệm; không được áp nội suy trễ cho xe người chơi rồi coi đó là cách giảm trễ phím.

## Thứ tự triển khai đề xuất

### 1. Bổ sung phép đo

- RTT ứng dụng qua `PingMessage`/`PongMessage`: median và p95; thời gian đến snapshot: median/p95 và khoảng ngắt dài nhất.
- Đo thời gian tick, encode, ghi socket và độ dài/tuổi hàng đợi.
- Đo thời gian chờ EDT và render; bytes/s theo phòng/client.
- Dùng đồng hồ đơn điệu để đo khoảng thời gian. Không so trực tiếp wall clock hai máy để suy ra độ trễ một chiều.
- Ghi tổng hợp định kỳ, tránh in log mỗi tick.

### 2. Tách ghi mạng khỏi EDT và tick

- Writer riêng cho từng kết nối, hàng đợi có giới hạn và cơ chế đóng socket để thoát khi writer kẹt.
- Snapshot có thể thay thế bằng bản mới hơn để tránh hiển thị dồn trạng thái cũ. Giữ thứ tự với JOIN/START/GAME OVER, không gửi snapshot ván cũ sau GAME OVER hoặc sang ván mới.
- Không làm mất âm thanh/sự kiện khi bỏ snapshot: snapshot hiện chứa `sounds`; cần bảo toàn các sự kiện này hoặc tách kênh sự kiện tin cậy trước khi coalescing snapshot.
- Input di chuyển có thể giữ trạng thái mới nhất; phải xử lý riêng nhấn/thả bắn và các thao tác rời phòng. Không bỏ tùy tiện một lần bấm bắn ngắn.
- Không sử dụng queue vô hạn để chuyển chỗ nghẽn sang bộ nhớ.

### 3. Vẽ mượt độc lập với nhịp nhận mạng

- Render Timer khoảng 60 FPS; đọc một snapshot/render frame nhất quán.
- Nội suy xe từ xa và đạn giữa các snapshot, thử đệm 50–100 ms rồi điều chỉnh theo jitter đo được. Đây là khoảng khởi đầu thử nghiệm.
- Giới hạn nội suy khi đổi hướng, respawn, dịch chuyển lớn, phá hủy và đổi ván để tránh xe xuyên tường hoặc trượt từ điểm chết tới điểm spawn.
- Cập nhật cache map khi snapshot/map thay đổi, không dựng lại map ở mỗi lần paint.
- Không kéo xe tiếp tục di chuyển vô hạn khi mất snapshot.

### 4. Giảm trễ phản hồi của xe mình

- Sau khi đo tác động các bước trên, triển khai local prediction và reconcile theo xác nhận input của server.
- Cần sequence input/ACK rõ ràng; `InputMessage.tick` hiện là tick snapshot client biết, chưa phải sequence input được ACK.
- Dùng cùng quy tắc di chuyển/va chạm và bước thời gian với server; server vẫn quyết định kết quả bắn, sát thương và điểm.
- Bắn có thể phản hồi hình ảnh/âm thanh cục bộ, nhưng phải đối chiếu để tránh phát trùng hoặc ghi nhận trúng đích giả.

### 5. Tối ưu lưu lượng sau khi có số đo

- Encode một lần cho cùng bản tin broadcast khi an toàn.
- Xem xét map ban đầu + delta tường/vật phẩm; cần phiên bản map, thứ tự và cơ chế đồng bộ khi join/rejoin.
- Chưa tăng tick lên 60: `GameEngine.update()` hiện di chuyển theo bước mỗi tick, nên đổi tick rate có thể đổi tốc độ xe, đạn và timer, đồng thời tăng lưu lượng.

## Kiểm chứng khi server hoạt động trở lại

1. Baseline: hai client cùng kết nối trực tiếp server thử nghiệm để xác định độ trễ không qua relay. Với VM NAT có thể thêm port-forward game chỉ bind host `127.0.0.1`, server lắng nghe giao diện VM phù hợp; không dùng IP localhost bên trong VM làm địa chỉ đích từ Windows.
2. Lặp cùng tình huống qua Pinggy, rồi từ hai mạng Internet khác nhau. So sánh RTT/jitter, tick và EDT trước/sau từng thay đổi.
3. Chơi 2 rồi 5 người; chạy thẳng, đổi hướng nhanh, nhấn/thả bắn ngắn, va tường, trượt băng, chết/respawn, rời phòng và chơi ván tiếp.
4. Giả lập một client đọc chậm để xác nhận tick và client khác không bị kéo chậm; kiểm tra queue có giới hạn và shutdown không treo.
5. Kiểm thử deterministic cho interpolation với clock tiêm vào; kiểm tra biên đổi hướng/teleport/respawn. Với prediction, kiểm tra reconcile không đổi tốc độ hoặc cho đi xuyên tường.

Tiêu chí: cải thiện đo được so với baseline, không đổi luật gameplay, không mất READY/START/GAME OVER hoặc sự kiện bắn. Chưa cam kết FPS hay RTT cụ thể khi chưa đo đường truyền thực.
