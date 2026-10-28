package tankops.server.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.server.game.RoomManager;

/**
 * Lớp chính của server: đọc cấu hình, mở cổng, giữ danh sách client đang kết nối.
 */
public class GameServer {

    private int port;
    private int maxClients;
    private RoomManager roomManager;
    private int tickRate;
    private String multiMapPath;
    private String bindAddress;


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

        String configPath = System.getProperty("tankops.config");
        if (configPath != null) {
            try (InputStream external = Files.newInputStream(Path.of(configPath))) {
                props.load(external);
            }
        }
        bindAddress = System.getProperty("tankops.bindAddress", props.getProperty("server.bindAddress", "0.0.0.0"));

        port = Integer.parseInt(props.getProperty("server.port", "5555"));
        maxClients = Integer.parseInt(props.getProperty("server.maxClients", "100"));

        int maxPlayers = Integer.parseInt(props.getProperty("room.maxPlayers", "5"));
        int maxRooms = Integer.parseInt(props.getProperty("server.maxRooms", "10"));
        tickRate = Integer.parseInt(props.getProperty("game.tickRate", "30"));
        multiMapPath = props.getProperty("game.multiMap", "/maps/map_multi.map");

        // Số phòng tạo sẵn khi mở server (cũng là số phòng chờ tối thiểu luôn được giữ)
        int defaultRooms = Integer.parseInt(props.getProperty("server.defaultRooms", "3"));
        roomManager = new RoomManager(maxRooms, maxPlayers, defaultRooms);
    }

    // Mở cổng và bật thread chuyên nhận kết nối mới
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(bindAddress));
        System.out.println("[GameServer] Đang lắng nghe tại " + bindAddress + ":" + serverSocket.getLocalPort());

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

    public int getTickRate() {
        return tickRate;
    }

    // Mỗi ván nạp bản đồ mới (engine sẽ sửa bản đồ trong lúc chơi, nên không dùng chung giữa các ván)
    public GameMap loadMultiMap() {
        return new MapLoader().loadMap(multiMapPath);
    }

    public synchronized boolean isFull() {
        return sessions.size() >= maxClients;
    }

    public synchronized int getSessionCount() {
        return sessions.size();
    }

    public int getMaxClients() {
        return maxClients;
    }
//getSessionCount có synchronized vì danh sách sessions bị nhiều thread đụng vào.
// getMaxClients thì không cần, vì giá trị chỉ được gán một lần lúc khởi động và không đổi nữa.


    public static void main(String[] args) throws IOException {
        GameServer server = new GameServer();
        server.start();
    }
}
