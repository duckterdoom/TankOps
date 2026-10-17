package tankops.server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Đại diện cho 1 client. Chạy trên thread riêng của nó.
 * Hiện tại chỉ "echo": client gửi gì thì server gửi lại y như vậy.
 */
public class ClientSession implements Runnable {

    // Bộ đếm để mỗi client có id riêng: player-1, player-2, ...
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final Socket socket;
    private final GameServer gameServer;
    private final String playerId = "player-" + NEXT_ID.getAndIncrement();

    // Ống gửi dữ liệu cho client. volatile: thread tick đọc được giá trị do thread này gán.
    private volatile PrintWriter out;

    public ClientSession(Socket socket, GameServer gameServer) {
        this.socket = socket;
        this.gameServer = gameServer;
    }

    public String getPlayerId() {
        return playerId;
    }

    /**
     * Gửi 1 dòng cho client. Thread tick và thread của client có thể gọi cùng lúc
     * nên phải synchronized để 2 dòng không bị trộn vào nhau.
     */
    public synchronized void send(String line) {
        PrintWriter w = out;
        if (w != null) {
            w.println(line);
        }
    }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true); // gửi gói nhỏ ngay, không chờ gộp
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true); // true = tự flush

            // TODO (T09/T14): thay đoạn echo này bằng đọc message theo protocol.md
            String line;
            while ((line = in.readLine()) != null) { // null nghĩa là client đã ngắt
                System.out.println("[ClientSession] Nhận: " + line);
                send(line);
            }

        } catch (IOException e) {
            System.out.println("[ClientSession] Client ngắt đột ngột: " + e.getMessage());
        } finally {
            // Dù kết thúc bình thường hay có lỗi, luôn dọn dẹp ở đây (đúng 1 lần)
            try {
                socket.close();
            } catch (IOException e) {
                // bỏ qua
            }
            gameServer.removeSession(this);
        }
    }
}