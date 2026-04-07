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

                databaseFile.seek(16); // Skip the first 16 bytes of the header
                byte[] pageSizeBytes = new byte[2]; // The following 2 bytes are the page size
                databaseFile.read(pageSizeBytes);
                short pageSizeSigned = ByteBuffer.wrap(pageSizeBytes).getShort();
                int pageSize = Short.toUnsignedInt(pageSizeSigned);
                byte[] pageHeader = new byte[2];
                databaseFile.seek(103);
                databaseFile.read(pageHeader);
                short numberOfTablesSigned = ByteBuffer.wrap(pageHeader).getShort();
                int numberOfTables = Short.toUnsignedInt(numberOfTablesSigned);

                IO.println("database page size: " + pageSize);
                IO.println("number of tables: " + numberOfTables);
            } catch (IOException e) {
                IO.println("Error reading file: " + e.getMessage());
            }
        }
        default -> IO.println("Missing or invalid command passed: " + command);
    }
}
