import java.io.IOException;
import java.io.RandomAccessFile;

public class SqliteSchemaCell {
    public int size;
    public int rowId;
    public String tblName;

    public SqliteSchemaCell(int size, int rowId, String tblName) {
        this.size = size;
        this.rowId = rowId;
        this.tblName = tblName;
    }

    public static SqliteSchemaCell fromBytes(RandomAccessFile databaseFile, byte[] bytes) throws IOException {
        var cellOffset = Utils.intFromByte(bytes);
        databaseFile.seek(cellOffset);
        int recordSize = Utils.readVarintFromBytes(databaseFile);
        int rowId = Utils.readVarintFromBytes(databaseFile);
        String table = Utils.readRecord(databaseFile);
        return new SqliteSchemaCell(recordSize, rowId, table);
    }
}
