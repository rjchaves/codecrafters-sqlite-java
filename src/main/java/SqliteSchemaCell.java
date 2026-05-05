import java.io.IOException;
import java.io.RandomAccessFile;


/**
 *  In SQLite internals, cell has a specific file-format meaning: it means an entry inside a b-tree page, not a spreadsheet-like single value.
 *  On a leaf page, each b-tree cell holds one key/payload entry, and for a table leaf page that payload is the serialized row record.
 * */
public class SqliteSchemaCell {
    public int size;
    public int rowId;
    public SchemaRecord schemaRecordData;

    public SqliteSchemaCell(int size, int rowId, SchemaRecord schemaRecordData) {
        this.size = size;
        this.rowId = rowId;
        this.schemaRecordData = schemaRecordData;
    }

    public static SqliteSchemaCell fromBytes(RandomAccessFile databaseFile, byte[] bytes) throws IOException {
        var cellOffset = Utils.intFromByte(bytes);
        databaseFile.seek(cellOffset);
        int recordSize = Utils.readVarintFromBytes(databaseFile);
        int rowId = Utils.readVarintFromBytes(databaseFile);
        SchemaRecord table = readSchemaRecord(databaseFile); // Cell contains record
        return new SqliteSchemaCell(recordSize, rowId, table);
    }

    //Record of a cell is actually a row
    public static SchemaRecord readSchemaRecord(RandomAccessFile databaseFile) throws IOException {
        long filePointer = databaseFile.getFilePointer();
        int recordHeaderSizeInBytes = Utils.readVarintFromBytes(databaseFile);
        int sizeOfType = Utils.readVarintFromBytes(databaseFile);
        int nameSize = Utils.readVarintFromBytes(databaseFile);
        int tblNameSize = Utils.readVarintFromBytes(databaseFile);
        /**
         * root page size is a varint, since we are using RandomAccessFile with a cursor we do not need to get the it's size
         * */
//        int rootPageSize = readVarintFromBytes(databaseFile);
        int sqlSize = Utils.readVarintFromBytes(databaseFile);
        byte[] type = new byte[Utils.getSerialTypeSize(sizeOfType)];
        byte[] name = new byte[Utils.getSerialTypeSize(nameSize)];
        byte[] tblName = new byte[Utils.getSerialTypeSize(tblNameSize)];

        byte[] sql = new byte[Utils.getSerialTypeSize(sqlSize)];
        databaseFile.seek(filePointer+recordHeaderSizeInBytes); // skip the header

        databaseFile.read(type);
        databaseFile.read(name);
        databaseFile.read(tblName);
        int rootPageOffset = Utils.readVarintFromBytes(databaseFile);
        databaseFile.read(sql);
        return new SchemaRecord(new String(type), new String(name), new String(tblName), rootPageOffset, new String(sql));
    }
}

