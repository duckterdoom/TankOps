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

        try {
            // Việc "tìm/tạo phòng + thêm người" làm trong RoomManager, trong một khối khóa,
            // để không bị phòng bị xóa chen vào giữa (tránh người chơi vào "phòng ma").
            GameRoom room = gameServer.getRoomManager().joinOrCreate(join.roomId(), this);
            int playerNumber = room.getPlayerCount();
            send(new JoinResponseMessage(true, "Vào phòng thành công", room.getId(), playerId, playerNumber));
            System.out.println("[ClientSession " + playerId + "] Đã vào phòng: " + room.getId());

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
            gameServer.getRoomManager().leaveRoom(currentRoom.getId(), this);
            currentRoom = null;
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
            room.submitMove(playerId, input.direction());
            if (input.shooting()) {
                room.submitShoot(playerId);
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
        if (START_COMMAND.equals(chat.content().trim())) {
            handleStartRequest();
            return;
        }
        GameRoom room = currentRoom;
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
        if (currentRoom != null && gameServer != null && gameServer.getRoomManager() != null) {
            try {
                gameServer.getRoomManager().leaveRoom(currentRoom.getId(), this);
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
}