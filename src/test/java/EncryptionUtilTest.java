import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.util.Arrays;

class EncryptionUtilTest {
    
    @Test
    @DisplayName("Salt generation should produce correct length")
    void testGenerateSalt() {
        byte[] salt = EncryptionUtil.generateSalt();
        assertEquals(EncryptionUtil.SALT_LENGTH, salt.length);
        
        // Two generated salts should be different (random)
        byte[] anotherSalt = EncryptionUtil.generateSalt();
        assertFalse(Arrays.equals(salt, anotherSalt));
    }
    
    @Test
    @DisplayName("IV generation should produce correct length")
    void testGenerateIV() {
        byte[] iv = EncryptionUtil.generateIV();
        assertEquals(EncryptionUtil.IV_LENGTH, iv.length);
        
        // Two generated IVs should be different (random)
        byte[] anotherIV = EncryptionUtil.generateIV();
        assertFalse(Arrays.equals(iv, anotherIV));
    }
    
    @Test
    @DisplayName("Key derivation should work with valid input")
    void testDeriveKey() throws Exception {
        String password = "testPassword123";
        byte[] salt = EncryptionUtil.generateSalt();
        
        SecretKey key = EncryptionUtil.deriveKey(password, salt);
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length); // 256 bits = 32 bytes
        
        // Same password and salt should produce the same key
        SecretKey key2 = EncryptionUtil.deriveKey(password, salt);
        assertArrayEquals(key.getEncoded(), key2.getEncoded());
        
        // Different password should produce different key
        SecretKey key3 = EncryptionUtil.deriveKey("differentPassword", salt);
        assertFalse(Arrays.equals(key.getEncoded(), key3.getEncoded()));
    }
    
    @Test
    @DisplayName("Encryption and decryption should work together")
    void testEncryptionDecryption() throws Exception {
        String password = "testPassword123";
        byte[] salt = EncryptionUtil.generateSalt();
        byte[] iv = EncryptionUtil.generateIV();
        SecretKey key = EncryptionUtil.deriveKey(password, salt);
        
        // Get ciphers
        Cipher encryptCipher = EncryptionUtil.getEncryptCipher(key, iv);
        Cipher decryptCipher = EncryptionUtil.getDecryptCipher(key, iv);
        
        // Test data
        byte[] testData = "Hello, World!".getBytes();
        
        // Encrypt
        byte[] encrypted = encryptCipher.doFinal(testData);
        assertFalse(Arrays.equals(testData, encrypted));
        
        // Decrypt
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        assertArrayEquals(testData, decrypted);
    }
    
    @Test
    @DisplayName("Encryption should fail with null key")
    void testEncryptionWithNullKey() {
        byte[] iv = EncryptionUtil.generateIV();
        assertThrows(Exception.class, () -> EncryptionUtil.getEncryptCipher(null, iv));
    }
    
    @Test
    @DisplayName("Decryption should fail with null IV")
    void testDecryptionWithNullIV() {
        String password = "testPassword123";
        byte[] salt = EncryptionUtil.generateSalt();
        assertThrows(Exception.class, () -> {
            SecretKey key = EncryptionUtil.deriveKey(password, salt);
            EncryptionUtil.getDecryptCipher(key, null);
        });
    }
} 