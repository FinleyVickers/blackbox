import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class BlackBox {
    private static final Scanner scanner = new Scanner(System.in);
    private static Map<String, StoredFile> files = new HashMap<>();
    private static String containerPath;
    private static String password;

    public static void main(String[] args) {
        showMainMenu();
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== BlackBox - Secure Storage ===");
            System.out.println("1. Create new container");
            System.out.println("2. Open existing container");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    createNewContainer();
                    break;
                case 2:
                    openExistingContainer();
                    break;
                case 3:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void createNewContainer() {
        System.out.println("\n=== Create New Container ===");
        containerPath = selectFile("Select location for new container", FileDialog.SAVE);
        if (containerPath == null) return;

        System.out.print("Enter encryption password: ");
        password = scanner.nextLine();

        files = new HashMap<>();
        System.out.println("Container created successfully!");
        containerMenu();
    }

    private static void openExistingContainer() {
        System.out.println("\n=== Open Container ===");
        containerPath = selectFile("Select container file", FileDialog.LOAD);
        if (containerPath == null) return;

        System.out.print("Enter password: ");
        password = scanner.nextLine();

        try {
            files = ContainerManager.loadContainer(containerPath, password);
            System.out.println("Container unlocked successfully!");
            containerMenu();
        } catch (Exception e) {
            System.out.println("Error opening container: " + e.getMessage());
        }
    }

    private static void containerMenu() {
        while (true) {
            System.out.println("\n=== Container Menu ===");
            System.out.println("1. Add files");
            System.out.println("2. View stored files");
            System.out.println("3. Extract file");
            System.out.println("4. Save and close");
            System.out.print("Choose option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    addFiles();
                    break;
                case 2:
                    listFiles();
                    break;
                case 3:
                    extractFile();
                    break;
                case 4:
                    saveAndClose();
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void addFiles() {
        String[] filePaths = selectMultipleFiles();
        if (filePaths == null) return;

        for (String filePath : filePaths) {
            try {
                Path path = Paths.get(filePath);
                String fileName = path.getFileName().toString();
                String fileType = Files.probeContentType(path) != null ?
                        Files.probeContentType(path) : "unknown";
                byte[] content = Files.readAllBytes(path);

                files.put(fileName, new StoredFile(fileName, fileType, content));
                System.out.println("Added: " + fileName);
            } catch (Exception e) {
                System.out.println("Error adding " + filePath + ": " + e.getMessage());
            }
        }
        saveContainer();
    }

    private static void listFiles() {
        System.out.println("\nStored Files:");
        if (files.isEmpty()) {
            System.out.println("No files in container");
            return;
        }

        int index = 1;
        for (StoredFile file : files.values()) {
            System.out.printf("%d. %-20s (%s)\n", index++, file.getName(), file.getType());
        }
    }

    private static void extractFile() {
        listFiles();
        if (files.isEmpty()) return;

        System.out.print("Enter file number to extract: ");
        int fileNumber = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        StoredFile file = files.values().toArray(new StoredFile[0])[fileNumber - 1];
        String savePath = selectFile("Select location to save file", FileDialog.SAVE);

        if (savePath != null) {
            try {
                Files.write(Paths.get(savePath), file.getContent());
                System.out.println("File extracted successfully!");
            } catch (Exception e) {
                System.out.println("Error extracting file: " + e.getMessage());
            }
        }
    }

    private static void saveAndClose() {
        saveContainer();
        files.clear();
        containerPath = null;
        password = null;
    }

    private static void saveContainer() {
        try {
            ContainerManager.saveContainer(containerPath, password, files);
            System.out.println("Container saved successfully!");
        } catch (Exception e) {
            System.out.println("Error saving container: " + e.getMessage());
        }
    }

    private static String selectFile(String title, int mode) {
        FileDialog fd = new FileDialog((Frame)null, title, mode);
        fd.setFilenameFilter((dir, name) -> true);
        fd.setVisible(true);

        if (fd.getFile() == null) return null;
        return Paths.get(fd.getDirectory(), fd.getFile()).toString();
    }

    private static String[] selectMultipleFiles() {
        FileDialog fd = new FileDialog((Frame)null, "Select files to encrypt", FileDialog.LOAD);
        fd.setMultipleMode(true);
        fd.setVisible(true);

        File[] selectedFiles = fd.getFiles();
        if (selectedFiles == null || selectedFiles.length == 0) return null;

        String[] paths = new String[selectedFiles.length];
        for (int i = 0; i < selectedFiles.length; i++) {
            paths[i] = selectedFiles[i].getAbsolutePath();
        }
        return paths;
    }
}