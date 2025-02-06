import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.util.function.Consumer;

public class StoredFile implements Serializable, AutoCloseable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String type;
    private transient Path tempFile;  // Stores compressed data in temp file

    public StoredFile(String name, String type, Path sourceFile, Consumer<Integer> progress)
            throws IOException {
        this.name = name;
        this.type = type;
        this.tempFile = compressToTemp(sourceFile, progress);
    }

    private Path compressToTemp(Path source, Consumer<Integer> progress) throws IOException {
        Path temp = Files.createTempFile("blackbox_", ".tmp");
        long fileSize = Files.size(source);

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(temp);
             GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                gzipOut.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                int progressPercent = (int) ((totalRead * 100) / fileSize);
                progress.accept(progressPercent);
            }
        }
        return temp;
    }

    public InputStream getContentStream() throws IOException {
        return new GZIPInputStream(Files.newInputStream(tempFile));
    }

    public long getTempFileSize() throws IOException {
        return Files.size(tempFile);
    }

    // Serialization handling
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        try (InputStream in = Files.newInputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.tempFile = Files.createTempFile("blackbox_", ".tmp");
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public void close() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
            tempFile = null;
        }
    }
}