import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.zip.*;

public class StoredFile implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String type;
    private final byte[] content;

    public StoredFile(String name, String type, byte[] content) {
        this.name = name;
        this.type = type;
        this.content = compress(content);
    }

    // Add back the missing getters
    public String getName() { return name; }
    public String getType() { return type; }
    public byte[] getContent() {
        return decompress(content);
    }

    private static byte[] compress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    private static byte[] decompress(byte[] compressed) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }
}