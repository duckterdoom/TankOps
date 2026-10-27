package tankops.server.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import tankops.common.net.Message;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.codec.ProtocolException;
import tankops.common.net.lobby.*;
import tankops.common.net.game.*;
import tankops.common.net.system.*;
import tankops.server.game.GameRoom;
import tankops.server.game.RoomFullException;
import tankops.server.game.RoomState;

/**
 * Đại diện cho 1 client kết nối tới Server. Chạy trên thread riêng.
 *
 * Yêu cầu kiến trúc:
 * - Mục 5.1: "ClientSession"
 * - Mục 6.3: "ClientSession: One instance per connected client. Runs on an independent thread,
 *             handles binary protocol via MessageCodec, dispatches inputs to GameRoom."
 * - Mục 6.4: "Binary I/O: Network messages are encoded with DataInputStream / DataOutputStream"
 *
 * Hỗ trợ tạo fake session cho unit test: new ClientSession(null, null).
 */
public class ClientSession implements Runnable {

    // Bộ đếm để mỗi client có ID riêng duy nhất: player-1, player-2, ...
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final Socket socket;
    private final GameServer gameServer;
    private final String playerId = "player-" + NEXT_ID.getAndIncrement();
    private volatile String playerName;

    // Stream nhị phân theo MessageCodec
    private volatile DataOutputStream dataOut;
    private volatile DataInputStream dataIn;

    // Phòng chơi hiện tại của client này (nếu đã vào phòng)
    private volatile GameRoom currentRoom;

    // Đã dọn dẹp chưa (để cleanup chỉ chạy 1 lần)
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ClientSession(Socket socket, GameServer gameServer) {
        this.socket = socket;
        this.gameServer = gameServer;
    }

    public String getPlayerId() {
        return playerId;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }

    // =========================================================================
    // SEND API (thread-safe)
    // =========================================================================

    /**
     * Gửi bản tin nhị phân tới client.
     * Đồng bộ trên dataOut để tránh xen kẽ dữ liệu giữa các luồng.
     */
    public boolean send(Message message) {
        DataOutputStream out = dataOut;
        if (out == null) {
            return false;
        }
        try {
            synchronized (out) {
                MessageCodec.writeMessage(out, message);
            }
            return true;
        } catch (IOException e) {
            System.err.println("[ClientSession " + playerId + "] Lỗi gửi message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi text thuần (giữ lại tương thích với mã cũ / test cũ).
     */
    public synchronized void send(String line) {
        // Nếu không có socket hoặc client chưa kết nối, bỏ qua an toàn
        if (socket == null || dataOut == null) {
            return;
        }
    }

    // =========================================================================
    // RUN LOOP
    // =========================================================================

    @Override
    public void run() {
        if (socket == null) {
            return; // Dành cho fakePlayer trong unit test
        }

        try {
            socket.setTcpNoDelay(true); // Gửi gói nhỏ ngay lập tức
            dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            while (!socket.isClosed()) {
                Message msg;
                try {
                    msg = MessageCodec.readMessage(dataIn);
                } catch (EOFException e) {
                    // Client ngắt kết nối bình thường
                    break;
                } catch (SocketException e) {
                    // Socket bị đóng từ bên ngoài
                    break;
                } catch (ProtocolException e) {
                    // TC07: Bản tin sai định dạng -> Từ chối, log cảnh báo, không crash server
                    System.err.println("[ClientSession " + playerId + "] Bản tin không hợp lệ: " + e.getMessage());
                    break;
                }

                handleMessage(msg);
            }

        } catch (IOException e) {
            System.out.println("[ClientSession " + playerId + "] Client ngắt kết nối: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // =========================================================================
    // DISPATCH MESSAGE
    // =========================================================================

    private void handleMessage(Message message) {
        switch (message) {
            case JoinRoomMessage join -> handleJoinRoom(join);
            case LeaveRoomMessage leave -> handleLeaveRoom(leave);
            case RoomListRequestMessage req -> handleRoomListRequest(req);
            case InputMessage input -> handleInput(input);
            case PingMessage ping -> send(new PongMessage(ping.timestamp()));
            case PongMessage pong -> { /* Client phản hồi ping, ghi nhận latency nếu cần */ }
            case ChatMessage chat -> handleChat(chat);
            case DisconnectMessage disc -> {
                System.out.println("[ClientSession " + playerId + "] Client chủ động ngắt: " + disc.reason());
                cleanup();
            }
            default -> {
                System.err.println("[ClientSession " + playerId + "] Chưa xử lý loại message: " + message.getClass().getSimpleName());
            }
        }
    }

    private void handleJoinRoom(JoinRoomMessage join) {
        if (gameServer == null || gameServer.getRoomManager() == null) {
            send(new JoinResponseMessage(false, "Server chưa sẵn sàng", join.roomId(), playerId, -1));
            return;
        }

        String requestName = (join.playerName() != null && !join.playerName().isBlank())
                ? join.playerName().trim()
                : null;
        if (requestName == null) {
            send(new JoinResponseMessage(false, "Tên người chơi không hợp lệ!", join.roomId(), playerId, -1));
            return;
        }

        try {
            // Việc "tìm/tạo phòng + thêm người" làm trong RoomManager, trong một khối khóa,
            // để không bị phòng bị xóa chen vào giữa (tránh người chơi vào "phòng ma").
            GameRoom room = gameServer.getRoomManager().joinOrCreate(join.roomId(), this);

            // Kiểm tra trùng tên trong phòng và đặt tên cho phiên này (làm trong khóa của phòng
            // để người khác không thấy tên "nửa chừng"). Chưa vào được phòng thì KHÔNG đổi tên.
            boolean duplicate = false;
            synchronized (room) {
                for (ClientSession other : room.getPlayers()) {
                    if (other != this && other.playerName != null
                            && requestName.equalsIgnoreCase(other.playerName.trim())) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    this.playerName = requestName;
                }
            }

            if (duplicate) {
                // Rời phòng qua RoomManager (đúng thứ tự khóa: RoomManager rồi mới tới GameRoom,
                // nên làm ở NGOÀI khối synchronized(room) ở trên).
                gameServer.getRoomManager().leaveRoom(room.getId(), this);
                send(new JoinResponseMessage(false,
                        "Tên '" + requestName + "' đã tồn tại trong phòng! Vui lòng đổi tên.",
                        join.roomId(), playerId, -1));
                return;
            }

            int playerNumber = room.getPlayerCount();
            send(new JoinResponseMessage(true, "Vào phòng thành công", room.getId(), getPlayerName(), playerNumber));
            System.out.println("[ClientSession " + getPlayerName() + "] Đã vào phòng: " + room.getId());

            // Báo danh sách người trong phòng cho tất cả mọi người
            room.broadcastRoomState();

        } catch (RoomFullException e) {
            send(new JoinResponseMessage(false, "Phòng đã đầy", join.roomId(), playerId, -1));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Không có phòng / phòng đã bắt đầu / đang ở phòng khác / hết số phòng tối đa
            send(new JoinResponseMessage(false, e.getMessage(), join.roomId(), playerId, -1));
        } catch (Exception e) {
            send(new JoinResponseMessage(false, "Lỗi vào phòng: " + e.getMessage(), join.roomId(), playerId, -1));
        }
    }

    private void handleLeaveRoom(LeaveRoomMessage leave) {
        if (currentRoom != null && gameServer != null) {
            GameRoom room = currentRoom;
            gameServer.getRoomManager().leaveRoom(currentRoom.getId(), this);
            currentRoom = null;

            // BROADCAST LẠI DANH SÁCH CHO NHỮNG NGƯỜI CÒN LẠI TRONG PHÒNG
            if (!room.isEmpty()) {
                room.broadcastRoomState();
            }
        }
    }

    private void handleRoomListRequest(RoomListRequestMessage req) {
        if (gameServer == null || gameServer.getRoomManager() == null) {
            send(new RoomListResponseMessage(List.of()));
            return;
        }

        List<GameRoom> rooms = gameServer.getRoomManager().listRooms();
        List<RoomInfo> infoList = new ArrayList<>();
        for (GameRoom r : rooms) {
            infoList.add(new RoomInfo(r.getId(), r.getPlayerCount(), r.getMaxPlayers(), r.getState().name()));
        }
        send(new RoomListResponseMessage(infoList));
    }

    private void handleInput(InputMessage input) {
        GameRoom room = currentRoom;
        if (room != null) {
            // Luôn dùng playerId của CHÍNH phiên này, không tin playerId client tự ghi trong tin nhắn
            room.submitMove(getPlayerName(), input.direction());
            if (input.shooting()) {
                room.submitShoot(getPlayerName());
            }
        }
    }

    // TẠM THỜI: protocol chưa có tin nhắn "bắt đầu ván". Trong lúc chờ M1 thêm StartGameMessage,
    // người chơi gõ chat "/start" để bắt đầu. Khi có StartGameMessage, bỏ nhánh này và
    // gọi handleStartRequest() từ handleMessage.
    private static final String START_COMMAND = "/start";

    private void handleStartRequest() {
        GameRoom room = currentRoom;
        if (room == null || gameServer == null || gameServer.getRoomManager() == null) {
            send(new ChatMessage("SERVER", "Bạn chưa ở trong phòng nào"));
            return;
        }

        // Kiểm tra xem người yêu cầu có phải Host (P1) không
        List<ClientSession> players = room.getPlayers();
        if (players.isEmpty() || !players.get(0).getPlayerId().equals(this.getPlayerId())) {
            send(new ChatMessage("SERVER", "Chỉ Host mới có quyền bắt đầu trận đấu!"));
            return;
        }

        // Kiểm tra xem tất cả Member đã Ready chưa
        if (!room.areAllMembersReady()) {
            send(new ChatMessage("SERVER", "Chưa thể bắt đầu! Cần ít nhất 2 người và tất cả Member phải READY."));
            return;
        }

        try {
            gameServer.getRoomManager().startGame(
                    room.getId(), this, gameServer.loadMultiMap(), gameServer.getTickRate());
            System.out.println("[ClientSession " + playerId + "] Bắt đầu ván ở phòng: " + room.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Chưa đủ người / phòng đã bắt đầu / không ở trong phòng đó...
            send(new ChatMessage("SERVER", e.getMessage()));
        }
    }

    private void handleChat(ChatMessage chat) {
        GameRoom room = currentRoom;
        if (room == null) return;

        String content = chat.content().trim();

        // 1. Xử lý lệnh Ready / Unready từ Client
        if ("READY".equalsIgnoreCase(content)) {
            room.setPlayerReady(getPlayerName(), true);
            System.out.println("[Room] " + getPlayerName() + " -> READY");
            room.broadcastRoomState(); // Broadcast để mọi người thấy icon/chữ Ready
            return;
        }
        else if ("UNREADY".equalsIgnoreCase(content)) {
            room.setPlayerReady(getPlayerName(), false);
            System.out.println("[Room] " + getPlayerName() + " -> UNREADY");
            room.broadcastRoomState();
            return;
        }

        if (START_COMMAND.equals(chat.content().trim())) {
            handleStartRequest();
            return;
        }

        if (room != null) {
            // Forward tin nhắn tới tất cả người chơi trong cùng phòng
            room.broadcast(new ChatMessage(playerId, chat.content()));
        }
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    private void cleanup() {
        // Có thể bị gọi 2 lần (tin Disconnect rồi finally). Chỉ làm đúng 1 lần.
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        GameRoom roomToUpdate = currentRoom;
        if (roomToUpdate != null && gameServer != null && gameServer.getRoomManager() != null) {
            try {
                gameServer.getRoomManager().leaveRoom(roomToUpdate.getId(), this);
                if (!roomToUpdate.isEmpty()) {
                    roomToUpdate.broadcastRoomState();
                }
            } catch (Exception ignored) {}
            currentRoom = null;
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }

        if (gameServer != null) {
            gameServer.removeSession(this);
        }
    }

    public String getPlayerName() {
        if (playerName != null && !playerName.isBlank()) {
            return playerName;
        }
        return playerId != null ? playerId : "Player_Unknown";
    }
}