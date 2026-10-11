package tankops.server.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import tankops.server.game.RoomManager;

/**
 * Lớp chính của server: đọc cấu hình, mở cổng, giữ danh sách client đang kết nối.
 */
public class GameServer {

    private int port;
    private int maxClients;
    private RoomManager roomManager;


    // Danh sách client đang kết nối.
    // Nhiều thread (acceptor + các ClientSession) cùng sửa danh sách này,
    // nên mọi hàm đụng tới nó đều có "synchronized".
    private final List<ClientSession> sessions = new ArrayList<>();

    // Đọc config.properties (nằm trong thư mục resources)
    public GameServer() throws IOException {
        Properties props = new Properties();
        InputStream in = GameServer.class.getResourceAsStream("/config.properties");
        if (in == null) {
            throw new IOException("Không tìm thấy config.properties");
        }
        props.load(in);
        in.close();

        port = Integer.parseInt(props.getProperty("server.port", "5555"));
        maxClients = Integer.parseInt(props.getProperty("server.maxClients", "100"));

        int maxPlayers = Integer.parseInt(props.getProperty("room.maxPlayers", "5"));
        int maxRooms = Integer.parseInt(props.getProperty("server.maxRooms", "10"));
        roomManager = new RoomManager(maxRooms, maxPlayers);
    }

    // Mở cổng và bật thread chuyên nhận kết nối mới
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("[GameServer] Đang lắng nghe ở cổng " + port);

        ConnectionAcceptor acceptor = new ConnectionAcceptor(serverSocket, this);
        new Thread(acceptor).start();
    }

    // synchronized: mỗi lúc chỉ 1 thread được chạy hàm này
    public synchronized void addSession(ClientSession session) {
        sessions.add(session);
        System.out.println("[GameServer] Client mới kết nối. Tổng: " + sessions.size());
    }

    public synchronized void removeSession(ClientSession session) {
        sessions.remove(session);
        System.out.println("[GameServer] Client ngắt kết nối. Còn lại: " + sessions.size());
    }


    public RoomManager getRoomManager() {
        return roomManager;
    }

    public synchronized boolean isFull() {
        return sessions.size() >= maxClients;
    }

    public static void main(String[] args) throws IOException {
        GameServer server = new GameServer();
        server.start();
    }
}