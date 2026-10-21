package tankops.common.net;

/**
 * Interface đại diện cho mọi bản tin mạng trong TankOps.
 *
 * Định dạng khung nhị phân chuẩn (Wire Format):
 * [Length: int32 (4 bytes Big-Endian)][TypeID: uint8 (1 byte)][Payload: (Length-1) bytes]
 */
public interface Message {

    /**
     * Trả về mã byte định danh loại bản tin (Type ID).
     */
    byte getTypeId();
}
