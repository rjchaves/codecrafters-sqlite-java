import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class Utils {

    public static int MAX_VARINT_SIZE = 9;


    /**
     * int can have 9 bytesRead, if first 8, take the least 7 bits if is last
     * take all the bits.
     * */
    public static int readVarintFromBytes(RandomAccessFile databaseFile) throws IOException {
        int result = 0;
        for (int i = 0; i < MAX_VARINT_SIZE; i++) {
            byte b = databaseFile.readByte();
            if (i == MAX_VARINT_SIZE - 1) {
                result = (result << 8) | b;
            } else {
                result = (result << 7) | (b & 0b01111111);
            }

            if ((b & 0b10000000) == 0) {
                break;
            }
        }
        return result;
    }

    public static String readRecord(RandomAccessFile databaseFile) throws IOException {
        long filePointer = databaseFile.getFilePointer();
        int recordHeaderSizeInBytes = readVarintFromBytes(databaseFile);
        int sizeOfType = readVarintFromBytes(databaseFile);
        int nameSizeType = readVarintFromBytes(databaseFile);
        int tblNameSizeType = readVarintFromBytes(databaseFile);
        int sqlType = readVarintFromBytes(databaseFile);

        databaseFile.seek(filePointer+recordHeaderSizeInBytes);

        databaseFile.skipBytes(getSerialTypeSize(sizeOfType) +
                getSerialTypeSize(nameSizeType));

        byte[] result = new byte[getSerialTypeSize(tblNameSizeType)];
        databaseFile.read(result);
        return new String(result);
    }

    // https://www.sqlite.org/fileformat.html#schema_layer
    public static int getSerialTypeSize(int serialType) {
        return switch (serialType) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 6;
            case 6 -> 8;
            case 7 -> 8;
            case 8 -> 0;
            case 9 -> 0;
            default -> {
                if (serialType >= 12) {
                    if (serialType % 2 == 0) {
                        yield (serialType - 12) / 2;
                    } else {
                        yield (serialType - 13) / 2;
                    }
                }
                yield -1;
            }
        };
    }

    public static int intFromByte(byte[] pageSizeBytes) {
        short pageSizeSigned = ByteBuffer.wrap(pageSizeBytes).getShort();
        return Short.toUnsignedInt(pageSizeSigned);
    }
}
