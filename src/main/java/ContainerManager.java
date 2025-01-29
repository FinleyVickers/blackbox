import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ContainerManager {
    public static Map<String, StoredFile> loadContainer(String containerPath, String password) throws Exception {
        if (!Files.exists(Paths.get(containerPath))) {
            return new java.util.HashMap<>();
        }

        try (InputStream is = new FileInputStream(containerPath)) {
            byte[] salt = new byte[EncryptionUtil.SALT_LENGTH];
            is.read(salt);

            byte[] iv = new byte[EncryptionUtil.IV_LENGTH];
            is.read(iv);

            byte[] encryptedData = is.readAllBytes();

            SecretKey key = EncryptionUtil.deriveKey(password, salt);
            byte[] decryptedData = EncryptionUtil.decrypt(encryptedData, key, iv);

            ByteArrayInputStream bis = new ByteArrayInputStream(decryptedData);
            ObjectInputStream ois = new ObjectInputStream(bis);
            @SuppressWarnings("unchecked")
            Map<String, StoredFile> files = (Map<String, StoredFile>) ois.readObject();
            return files;
        }
    }

    public static void saveContainer(String containerPath, String password, Map<String, StoredFile> files) throws Exception {
        byte[] salt;
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

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(files);
        oos.close();
        byte[] data = bos.toByteArray();

        byte[] encryptedData = EncryptionUtil.encrypt(data, key, iv);

        try (OutputStream os = new FileOutputStream(containerPath)) {
            os.write(salt);
            os.write(iv);
            os.write(encryptedData);
        }
    }
}