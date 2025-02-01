import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class ContainerManager {
    public static void saveContainer(String containerPath, String password, Map<String, StoredFile> files) throws Exception {
        try {
            // Validate path accessibility
            Path container = Paths.get(containerPath);
            if (!Files.isWritable(container.getParent())) {
                throw new IOException("No write permission for directory: " + container.getParent());
            }

            byte[] salt = Files.exists(Paths.get(containerPath))
                    ? readExistingSalt(containerPath)
                    : EncryptionUtil.generateSalt();

            byte[] iv = EncryptionUtil.generateIV();
            SecretKey key = EncryptionUtil.deriveKey(password, salt);

            try (OutputStream os = new FileOutputStream(containerPath);
                 CipherOutputStream cipherOut = new CipherOutputStream(os,
                         EncryptionUtil.getEncryptCipher(key, iv))) {

                os.write(salt);
                os.write(iv);

                try (ObjectOutputStream oos = new ObjectOutputStream(cipherOut)) {
                    oos.writeObject(files);
                }
            }
        } catch (Exception e) {
            throw new Exception("Save error: " + e.getMessage(), e);
        }
    }

    private static byte[] readExistingSalt(String path) throws IOException {
        try (InputStream is = new FileInputStream(path)) {
            byte[] salt = new byte[EncryptionUtil.SALT_LENGTH];
            if (is.read(salt) != salt.length) throw new IOException("Invalid salt");
            return salt;
        }
    }

    public static Map<String, StoredFile> loadContainer(String containerPath, String password) throws Exception {
        try (InputStream is = new FileInputStream(containerPath)) {
            byte[] salt = new byte[EncryptionUtil.SALT_LENGTH];
            is.read(salt);

            byte[] iv = new byte[EncryptionUtil.IV_LENGTH];
            is.read(iv);

            SecretKey key = EncryptionUtil.deriveKey(password, salt);
            Cipher cipher = EncryptionUtil.getDecryptCipher(key, iv);

            try (CipherInputStream cis = new CipherInputStream(is, cipher);
                 ObjectInputStream ois = new ObjectInputStream(cis)) {

                return (Map<String, StoredFile>) ois.readObject();
            }
        }
    }
}