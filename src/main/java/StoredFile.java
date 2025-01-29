import java.io.Serializable;

public class StoredFile implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
    private byte[] content;

    public StoredFile(String name, String type, byte[] content) {
        this.name = name;
        this.type = type;
        this.content = content;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public byte[] getContent() { return content; }
}