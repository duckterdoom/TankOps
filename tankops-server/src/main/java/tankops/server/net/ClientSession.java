package tankops.server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Đại diện cho 1 client. Chạy trên thread riêng của nó.
 * Hiện tại chỉ "echo": client gửi gì thì server gửi lại y như vậy.
 */
public class ClientSession implements Runnable {

    private final Socket socket;
    private final GameServer gameServer;

    public ClientSession(Socket socket, GameServer gameServer) {
        this.socket = socket;
        this.gameServer = gameServer;
    }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true); // gửi gói nhỏ ngay, không chờ gộp
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // true = tự flush

            // TODO (T09/T14): thay đoạn echo này bằng đọc message theo protocol.md
            String line;
            while ((line = in.readLine()) != null) { // null nghĩa là client đã ngắt
                System.out.println("[ClientSession] Nhận: " + line);
                out.println(line);
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