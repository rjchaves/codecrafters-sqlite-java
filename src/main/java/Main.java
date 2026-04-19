void main(String[] args) {
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
        case ".tables" -> {
            try (RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "r")) {
                int databaseSchemaCells = getDatabaseSchemaCells(databaseFile);
                databaseFile.seek(108); // we know it is a leaf page so 8 bytesRead header
                byte[][] cellsOffsets = new byte[databaseSchemaCells][];
                for (int i = 0; i < databaseSchemaCells; i++) {
                    cellsOffsets[i] = new byte[2];
                    databaseFile.read(cellsOffsets[i]);
                }

                String result = Stream.of(cellsOffsets)
                        .map(it -> {
                            try {
                                return Cell.fromBytes(databaseFile, 0, it).tblName;
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).reduce("", (a, b) -> a + " " + b);
                IO.println(result);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        default -> IO.println("Missing or invalid command passed: " + command);
    }
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
