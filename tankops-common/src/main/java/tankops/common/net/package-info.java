/**
 * Giao thức mạng nhị phân chuẩn TankOps (TCP Binary Protocol).
 *
 * <h2>Kiến trúc phân chia Package (Package Structure)</h2>
 * <ul>
 *   <li>{@link tankops.common.net.Message} - Hợp đồng gốc chung cho mọi bản tin mạng</li>
 *   <li>{@code tankops.common.net.codec} - Bộ mã hóa / giải mã nhị phân và ngoại lệ giao thức</li>
 *   <li>{@code tankops.common.net.lobby} - Các bản tin quản lý phòng chơi, sảnh chờ và danh sách phòng</li>
 *   <li>{@code tankops.common.net.game} - Các bản tin thời gian thực trong trận (Input, Snapshot, Event, GameOver)</li>
 *   <li>{@code tankops.common.net.system} - Các bản tin duy trì kết nối (Ping/Pong), Chat và Disconnect</li>
 * </ul>
 */
package tankops.common.net;
