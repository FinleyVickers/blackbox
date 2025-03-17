import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

class ContainerManagerTest {
    private Path tempContainerPath;
    private Path tempSourceFile;
    private static final String TEST_PASSWORD = "testPassword123";
    private static final String TEST_CONTENT = "Test file content";
    private Map<String, StoredFile> testFiles;
    private final AtomicInteger lastProgress = new AtomicInteger(0);
    private final Consumer<Integer> progressConsumer = progress -> lastProgress.set(progress);
    
    @BeforeEach
    void setUp() throws IOException {
        // Create temporary paths
        tempContainerPath = Files.createTempFile("test_container_", ".box");
        Files.delete(tempContainerPath); // Delete the file so we start fresh
        tempSourceFile = Files.createTempFile("test_source_", ".txt");
        
        // Write test content to source file
        Files.write(tempSourceFile, TEST_CONTENT.getBytes());
        
        // Create test files map
        testFiles = new HashMap<>();
        lastProgress.set(0);
        testFiles.put("test1.txt", new StoredFile("test1.txt", "text/plain", tempSourceFile, progressConsumer));
        testFiles.put("test2.txt", new StoredFile("test2.txt", "text/plain", tempSourceFile, progressConsumer));
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up temporary files
        Files.deleteIfExists(tempContainerPath);
        Files.deleteIfExists(tempSourceFile);
        
        // Close stored files
        for (StoredFile file : testFiles.values()) {
            file.close();
        }
    }
    
    @Test
    @DisplayName("Container should save and load files correctly")
    void testSaveAndLoadContainer() throws Exception {
        // Save container
        ContainerManager.saveContainer(tempContainerPath.toString(), TEST_PASSWORD, testFiles);
        assertTrue(Files.exists(tempContainerPath));
        
        // Load container
        Map<String, StoredFile> loadedFiles = ContainerManager.loadContainer(
            tempContainerPath.toString(), TEST_PASSWORD);
        
        // Verify loaded files
        assertNotNull(loadedFiles);
        assertEquals(testFiles.size(), loadedFiles.size());
        
        // Verify file contents
        for (Map.Entry<String, StoredFile> entry : loadedFiles.entrySet()) {
            assertTrue(testFiles.containsKey(entry.getKey()));
            StoredFile originalFile = testFiles.get(entry.getKey());
            StoredFile loadedFile = entry.getValue();
            
            assertEquals(originalFile.getName(), loadedFile.getName());
            assertEquals(originalFile.getType(), loadedFile.getType());
            
            // Compare contents
            try (InputStream originalStream = originalFile.getContentStream();
                 InputStream loadedStream = loadedFile.getContentStream()) {
                assertArrayEquals(
                    originalStream.readAllBytes(),
                    loadedStream.readAllBytes()
                );
            }
        }
    }
    
    @Test
    @DisplayName("Container should handle wrong password")
    void testWrongPassword() throws Exception {
        // Save container
        ContainerManager.saveContainer(tempContainerPath.toString(), TEST_PASSWORD, testFiles);
        
        // Try to load with wrong password
        assertThrows(Exception.class, () ->
            ContainerManager.loadContainer(tempContainerPath.toString(), "wrongPassword")
        );
    }
    
    @Test
    @DisplayName("Container should handle empty file map")
    void testEmptyContainer() throws Exception {
        Map<String, StoredFile> emptyFiles = new HashMap<>();
        
        // Save empty container
        ContainerManager.saveContainer(tempContainerPath.toString(), TEST_PASSWORD, emptyFiles);
        
        // Load empty container
        Map<String, StoredFile> loadedFiles = ContainerManager.loadContainer(
            tempContainerPath.toString(), TEST_PASSWORD);
        
        assertNotNull(loadedFiles);
        assertTrue(loadedFiles.isEmpty());
    }
    
    @Test
    @DisplayName("Container should handle non-existent file")
    void testNonExistentContainer() {
        Path nonExistentPath = Paths.get("non_existent_container.box");
        
        assertThrows(FileNotFoundException.class, () ->
            ContainerManager.loadContainer(nonExistentPath.toString(), TEST_PASSWORD)
        );
    }
    
    @Test
    @DisplayName("Container should handle invalid container format")
    void testInvalidContainerFormat() throws Exception {
        // Create an invalid container file
        Files.write(tempContainerPath, "Invalid content".getBytes());
        
        assertThrows(Exception.class, () ->
            ContainerManager.loadContainer(tempContainerPath.toString(), TEST_PASSWORD)
        );
    }
    
    @Test
    @DisplayName("Container should preserve file when saving with same salt")
    void testSaveWithExistingSalt() throws Exception {
        // First save
        ContainerManager.saveContainer(tempContainerPath.toString(), TEST_PASSWORD, testFiles);
        byte[] originalFileContent = Files.readAllBytes(tempContainerPath);
        
        // Second save with same files
        ContainerManager.saveContainer(tempContainerPath.toString(), TEST_PASSWORD, testFiles);
        byte[] newFileContent = Files.readAllBytes(tempContainerPath);
        
        // The IV will be different, so files won't be identical, but they should be similar in size
        assertTrue(Math.abs(originalFileContent.length - newFileContent.length) < 100);
    }
} 