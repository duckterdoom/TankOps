package tankops.server.game;

/**
 * 3 trạng thái trong vòng đời của một phòng:
 * LOBBY (đang chờ người) -> PLAYING (đang đánh nhau) -> FINISHED (đã kết thúc).
 * Chỉ đi theo chiều mũi tên, không quay ngược lại.
 */
public enum RoomState {
    LOBBY,
    PLAYING,
    FINISHED
}