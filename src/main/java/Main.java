void main(String[] args) throws IOException {
    if (args.length < 2) {
        IO.println("Missing <database path> and <command>");
        return;
    }

    String databaseFilePath = args[0];
    String command = args[1];

    switch (command) {
        case ".dbinfo" -> {
            try (RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "r")) {

                int pageSize = getDatabasePageSize(databaseFile);
                int numberOfTables = getDatabaseSchemaCells(databaseFile);

                IO.println("database page size: " + pageSize);
                IO.println("number of tables: " + numberOfTables);
            } catch (IOException e) {
                IO.println("Error reading file: " + e.getMessage());
            }
        }
        case ".tables" -> findAndPrintTableNames(databaseFilePath);

        // we are not building any AST for now we will keep it simple for the test cases
        case String s when s.contains("select count(*) from") -> {
            String[] s1 = s.split(" ");
            var table = s1[s1.length - 1];
            try (RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "r")) {
                SqliteSchemaCell[] allSchemaCells = findAllSchemaCells(databaseFile);
                Optional<Integer> rootPage = Arrays.stream(allSchemaCells)
                        .filter(it -> it.schemaRecordData.tblName().equals(table))
                        .findFirst()
                        .map(it -> it.schemaRecordData.rootPage());
                if(rootPage.isEmpty()) return;
                int rootPageOffset = (rootPage.get() - 1) * getDatabasePageSize(databaseFile);
                databaseFile.seek(rootPageOffset+3); // skips 1 byte of page type + 2 freeblock offset to get the 2 bytes cell number
                byte[] numberOfCells = new byte[2];
                databaseFile.read(numberOfCells);
                IO.println(Utils.intFromByte(numberOfCells));
            }
        }

        default -> IO.println("Missing or invalid command passed: " + command);
    }
}

private static void findAndPrintTableNames(String databaseFilePath) {
    try (RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "r")) {
        SqliteSchemaCell[] cellsOffsets = findAllSchemaCells(databaseFile);
        String result = Stream.of(cellsOffsets)
                .map(it -> it.schemaRecordData.tblName())
                .reduce("", (a, b) -> a + " " + b);
        IO.println(result);

    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

private static SqliteSchemaCell[] findAllSchemaCells(RandomAccessFile databaseFile) throws IOException {
    int databaseSchemaCells = getDatabaseSchemaCells(databaseFile);
    databaseFile.seek(108); // we know it is a leaf page so 8 bytesRead header
    byte[][] cellsOffsets = new byte[databaseSchemaCells][];
    for (int i = 0; i < databaseSchemaCells; i++) {
        cellsOffsets[i] = new byte[2];
        databaseFile.read(cellsOffsets[i]);
    }
    SqliteSchemaCell[] schemaCells = new SqliteSchemaCell[cellsOffsets.length];
    for (int i = 0; i < cellsOffsets.length; i++) {
        schemaCells[i] = SqliteSchemaCell.fromBytes(databaseFile, cellsOffsets[i]);
    }
    return schemaCells;
}

private static int getDatabaseSchemaCells(RandomAccessFile databaseFile) throws IOException {
    byte[] pageHeader = new byte[2];
    databaseFile.seek(103); // first 100 bytesRead start of page-1 b-tree header + 1 byte of page type + 2 freeblock offset
    databaseFile.read(pageHeader);
    return Utils.intFromByte(pageHeader);
}

private static int getDatabasePageSize(RandomAccessFile databaseFile) throws IOException {
    databaseFile.seek(16); // Skip the first 16 bytesRead of the header magic string
    byte[] pageSizeBytes = new byte[2]; // The following 2 bytesRead are the page size
    databaseFile.read(pageSizeBytes);
    return Utils.intFromByte(pageSizeBytes);
}
