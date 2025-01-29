import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

public class BlackBox {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter container file path:");
        String containerPath = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        Map<String, StoredFile> files;
        try {
            files = ContainerManager.loadContainer(containerPath, password);
        } catch (Exception e) {
            System.out.println("Error loading container: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Add file");
            System.out.println("2. Retrieve file");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");
            int option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1:
                    System.out.println("Enter file path to add:");
                    String filePath = scanner.nextLine();
                    System.out.println("Enter file type (image, video, document):");
                    String fileType = scanner.nextLine();
                    try {
                        byte[] content = Files.readAllBytes(Paths.get(filePath));
                        String fileName = Paths.get(filePath).getFileName().toString();
                        StoredFile storedFile = new StoredFile(fileName, fileType, content);
                        files.put(fileName, storedFile);
                        ContainerManager.saveContainer(containerPath, password, files);
                        System.out.println("File added successfully.");
                    } catch (Exception e) {
                        System.out.println("Error adding file: " + e.getMessage());
                    }
                    break;
                case 2:
                    System.out.println("Enter file name to retrieve:");
                    String fileName = scanner.nextLine();
                    StoredFile storedFile = files.get(fileName);
                    if (storedFile == null) {
                        System.out.println("File not found.");
                        break;
                    }
                    System.out.println("Enter output path:");
                    String outputPath = scanner.nextLine();
                    try {
                        Files.write(Paths.get(outputPath), storedFile.getContent());
                        System.out.println("File retrieved successfully.");
                    } catch (Exception e) {
                        System.out.println("Error retrieving file: " + e.getMessage());
                    }
                    break;
                case 3:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}