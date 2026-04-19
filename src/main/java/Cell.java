import java.io.IOException;
import java.io.RandomAccessFile;

public class Cell {
    public int size;
    public int rowId;
    public String tblName;

    public Cell(int size, int rowId, String tblName) {
        this.size = size;
        this.rowId = rowId;
        this.tblName = tblName;
    }

    public static Cell fromBytes(RandomAccessFile databaseFile, int pageOffset, byte[] bytes) throws IOException {
        var cellOffset = pageOffset + Utils.intFromByte(bytes);
        databaseFile.seek(cellOffset);
        int recordSize = Utils.readVarintFromBytes(databaseFile);
        int rowId = Utils.readVarintFromBytes(databaseFile);
        String table = Utils.readRecord(databaseFile);
        return new Cell(recordSize, rowId, table);
    }
}
