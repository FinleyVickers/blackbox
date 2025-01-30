import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.*;

public class ContainerManager {
    public static Map<String, StoredFile> loadContainer(String containerPath, String password) throws Exception {
        try (InputStream is = new FileInputStream(containerPath)) {
            // Read salt
            byte[] salt = new byte[EncryptionUtil.SALT_LENGTH];
            if (is.read(salt) != salt.length) {
                throw new IOException("Invalid container format");
            }

            // Read IV
            byte[] iv = new byte[EncryptionUtil.IV_LENGTH];
            if (is.read(iv) != iv.length) {
                throw new IOException("Invalid container format");
            }

            // Read encrypted data
            byte[] encryptedData = is.readAllBytes();

            // Derive key and decrypt
            SecretKey key = EncryptionUtil.deriveKey(password, salt);
            try {
                byte[] decryptedData = EncryptionUtil.decrypt(encryptedData, key, iv);
                return deserializeFiles(decryptedData);
            } catch (BadPaddingException e) {
                throw new InvalidKeyException("Invalid password");
            }
        }
    }

    public static void saveContainer(String containerPath, String password, Map<String, StoredFile> files) throws Exception {
        byte[] salt;
        // Read existing salt if container exists
        if (Files.exists(Paths.get(containerPath))) {
            try (InputStream is = new FileInputStream(containerPath)) {
                salt = new byte[EncryptionUtil.SALT_LENGTH];
                is.read(salt);
            }
        } else {
            salt = EncryptionUtil.generateSalt();
        }

        byte[] iv = EncryptionUtil.generateIV();
        SecretKey key = EncryptionUtil.deriveKey(password, salt);

        // Serialize files with compression
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);
             ObjectOutputStream oos = new ObjectOutputStream(gzip)) {
            oos.writeObject(files);
        }
        byte[] data = bos.toByteArray();

        // Encrypt and write
        byte[] encryptedData = EncryptionUtil.encrypt(data, key, iv);
        try (OutputStream os = new FileOutputStream(containerPath)) {
            os.write(salt);
            os.write(iv);
            os.write(encryptedData);
        }
    }

    private static Map<String, StoredFile> deserializeFiles(byte[] data) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(
                new GZIPInputStream(new ByteArrayInputStream(data)))) {
            @SuppressWarnings("unchecked")
            Map<String, StoredFile> files = (Map<String, StoredFile>) ois.readObject();
            return files;
        }
    }
}