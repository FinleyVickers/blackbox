import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

class StoredFileTest {
    private Path tempSourceFile;
    private static final String TEST_CONTENT = "This is test content that will be compressed. ".repeat(1000);
    private static final String TEST_FILENAME = "test.txt";
    private static final String TEST_TYPE = "text/plain";
    private final AtomicInteger lastProgress = new AtomicInteger(0);
    private final Consumer<Integer> progressConsumer = progress -> lastProgress.set(progress);
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary source file for testing
        tempSourceFile = Files.createTempFile("test_source_", ".txt");
        Files.write(tempSourceFile, TEST_CONTENT.getBytes());
        lastProgress.set(0);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up temporary files
        Files.deleteIfExists(tempSourceFile);
    }
    
    @Test
    @DisplayName("StoredFile should properly compress content")
    void testCompression() throws IOException {
        try (StoredFile storedFile = new StoredFile(TEST_FILENAME, TEST_TYPE, tempSourceFile, progressConsumer)) {
            // Verify progress was reported
            assertTrue(lastProgress.get() > 0, "Progress should be reported");
            
            // Check if compressed size is less than original (or equal for very small files)
            long compressedSize = storedFile.getTempFileSize();
            long originalSize = Files.size(tempSourceFile);
            assertTrue(compressedSize <= originalSize);
            
            // Verify content can be read back correctly
            try (InputStream contentStream = storedFile.getContentStream()) {
                byte[] decompressedContent = contentStream.readAllBytes();
                assertEquals(TEST_CONTENT, new String(decompressedContent));
            }
        }
    }
    
    @Test
    @DisplayName("StoredFile should handle serialization")
    void testSerialization() throws IOException, ClassNotFoundException {
        StoredFile originalFile = new StoredFile(TEST_FILENAME, TEST_TYPE, tempSourceFile, progressConsumer);
        
        // Serialize
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(originalFile);
            serialized = baos.toByteArray();
        }
        
        // Deserialize
        StoredFile deserializedFile;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            deserializedFile = (StoredFile) ois.readObject();
        }
        
        // Verify deserialized object
        assertEquals(TEST_FILENAME, deserializedFile.getName());
        assertEquals(TEST_TYPE, deserializedFile.getType());
        
        // Verify content
        try (InputStream contentStream = deserializedFile.getContentStream()) {
            byte[] decompressedContent = contentStream.readAllBytes();
            assertEquals(TEST_CONTENT, new String(decompressedContent));
        }
        
        // Clean up
        originalFile.close();
        deserializedFile.close();
    }
    
    @Test
    @DisplayName("StoredFile should clean up temporary files on close")
    void testCleanup() throws IOException {
        Path tempFilePath = null;
        StoredFile storedFile = new StoredFile(TEST_FILENAME, TEST_TYPE, tempSourceFile, progressConsumer);
        
        // Get the temp file path through reflection (since it's private)
        try {
            var field = StoredFile.class.getDeclaredField("tempFile");
            field.setAccessible(true);
            tempFilePath = (Path) field.get(storedFile);
            assertTrue(Files.exists(tempFilePath));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access tempFile field");
        }
        
        storedFile.close();
        assertFalse(Files.exists(tempFilePath));
    }
    
    @Test
    @DisplayName("StoredFile should handle empty files")
    void testEmptyFile() throws IOException {
        // Create empty file
        Files.write(tempSourceFile, new byte[0]);
        
        try (StoredFile storedFile = new StoredFile(TEST_FILENAME, TEST_TYPE, tempSourceFile, progressConsumer)) {
            try (InputStream contentStream = storedFile.getContentStream()) {
                byte[] content = contentStream.readAllBytes();
                assertEquals(0, content.length);
            }
        }
    }
    
    @Test
    @DisplayName("StoredFile should throw exception for non-existent source file")
    void testNonExistentFile() {
        Path nonExistentFile = Paths.get("non_existent_file.txt");
        assertThrows(IOException.class, () -> 
            new StoredFile(TEST_FILENAME, TEST_TYPE, nonExistentFile, progressConsumer)
        );
    }
} 