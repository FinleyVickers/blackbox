import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class BlackBox {
    private static Map<String, StoredFile> files = new HashMap<>();
    private static String containerPath;
    private static String password;
    private static JFrame activeContainerFrame; // Track the active container window

    public static void main(String[] args) {
        setupLookAndFeel();
        SwingUtilities.invokeLater(BlackBox::createAndShowMainUI);
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
    }

    private static void createAndShowMainUI() {
        JFrame frame = new JFrame("BlackBox - Secure Storage");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create buttons with hover effect
        JButton createBtn = new JButton("Create New Container");
        JButton openBtn = new JButton("Open Existing Container");
        JButton exitBtn = new JButton("Exit");

        createBtn.addActionListener(e -> createNewContainer());
        openBtn.addActionListener(e -> openExistingContainer());
        exitBtn.addActionListener(e -> System.exit(0));

        panel.add(createBtn);
        panel.add(openBtn);
        panel.add(exitBtn);

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void createNewContainer() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select location for new container");
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        containerPath = chooser.getSelectedFile().getAbsolutePath();
        password = getPasswordFromDialog("Enter encryption password:");

        if (password != null) {
            files = new HashMap<>();
            JOptionPane.showMessageDialog(null, "Container created successfully!");
            showContainerUI();
        } else {
            JOptionPane.showMessageDialog(null, "Container creation canceled.", "Error", JOptionPane.ERROR_MESSAGE);
            containerPath = null; // Reset path if password was canceled
        }
    }

    private static void openExistingContainer() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select container file");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

        containerPath = chooser.getSelectedFile().getAbsolutePath();
        password = getPasswordFromDialog("Enter password:");

        if (password != null) {
            showLoading(progress -> {
                try {
                    files = ContainerManager.loadContainer(containerPath, password);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Container unlocked successfully!");
                        showContainerUI();
                    });
                } catch (Exception ex) {
                    throw new RuntimeException(ex.getMessage());
                }
            }, "Decrypting container...");
        }
    }

    private static String getPasswordFromDialog(String prompt) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(prompt);
        JPasswordField passField = new JPasswordField(20);

        panel.add(label, BorderLayout.NORTH);
        panel.add(passField, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                null, panel, "Password",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        return (result == JOptionPane.OK_OPTION) ? new String(passField.getPassword()) : null;
    }

    private static void showContainerUI() {
        // Close existing container window if open
        if (activeContainerFrame != null) {
            activeContainerFrame.dispose();
        }

        activeContainerFrame = new JFrame("Container Management");
        activeContainerFrame.setSize(500, 300);
        activeContainerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton addBtn = new JButton("Add Files");
        JButton listBtn = new JButton("View Stored Files");
        JButton openBtn = new JButton("Open File");
        JButton extractBtn = new JButton("Extract File");
        JButton saveBtn = new JButton("Save and Close");

        addBtn.addActionListener(e -> addFiles());
        listBtn.addActionListener(e -> listFiles());
        openBtn.addActionListener(e -> openFile());
        extractBtn.addActionListener(e -> extractFile());
        saveBtn.addActionListener(e -> saveAndClose(activeContainerFrame));

        panel.add(addBtn);
        panel.add(listBtn);
        panel.add(openBtn);
        panel.add(extractBtn);
        panel.add(saveBtn);

        activeContainerFrame.add(panel);
        activeContainerFrame.setLocationRelativeTo(null);
        activeContainerFrame.setVisible(true);
    }

    private static void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select files to encrypt");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

        File[] selectedFiles = chooser.getSelectedFiles();
        showLoading(progress -> {
            for (int i = 0; i < selectedFiles.length; i++) {
                File file = selectedFiles[i];
                try {
                    Path path = file.toPath();

                    int finalI = i;
                    files.put(
                            path.getFileName().toString(),
                            new StoredFile(
                                    path.getFileName().toString(),
                                    Files.probeContentType(path) != null ?
                                            Files.probeContentType(path) : "unknown",
                                    path,
                                    p -> progress.accept((finalI * 100 + p) / selectedFiles.length)
                            )
                    );

                    ContainerManager.saveContainer(containerPath, password, files);
                } catch (Exception e) {
                    throw new RuntimeException("Error adding file: " + e.getMessage());
                }
            }
        }, "Encrypting files...");
    }

    private static void listFiles() {
        JFrame listFrame = new JFrame("Stored Files");
        listFrame.setSize(400, 300);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (StoredFile file : files.values()) {
            listModel.addElement(file.getName() + " (" + file.getType() + ")");
        }

        JList<String> fileList = new JList<>(listModel);
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        
        listFrame.add(scrollPane);
        listFrame.setLocationRelativeTo(null);
        listFrame.setVisible(true);
    }

    private static void extractFile() {
        List<StoredFile> fileList = new ArrayList<>(files.values());
        if (fileList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No files in container");
            return;
        }

        String[] options = fileList.stream()
                .map(f -> f.getName() + " (" + f.getType() + ")")
                .toArray(String[]::new);

        String selection = (String) JOptionPane.showInputDialog(
                null, "Select file to extract:", "Extract File",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]
        );

        if (selection == null) return;
        int index = Arrays.asList(options).indexOf(selection);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select save location");
        chooser.setSelectedFile(new File(fileList.get(index).getName()));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            showLoading(progress -> {
                try (InputStream in = fileList.get(index).getContentStream();
                     OutputStream out = Files.newOutputStream(chooser.getSelectedFile().toPath())) {

                    long fileSize = fileList.get(index).getTempFileSize();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        int progressPercent = (int) ((totalRead * 100) / fileSize);
                        progress.accept(progressPercent);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Extraction failed: " + ex.getMessage());
                }
            }, "Extracting file...");
        }
    }

    private static void openFile() {
        List<StoredFile> fileList = new ArrayList<>(files.values());
        if (fileList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No files in container");
            return;
        }

        String[] options = fileList.stream()
                .map(f -> f.getName() + " (" + f.getType() + ")")
                .toArray(String[]::new);

        String selection = (String) JOptionPane.showInputDialog(
                null, "Select file to open:", "Open File",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]
        );

        if (selection == null) return;
        int index = Arrays.asList(options).indexOf(selection);

        showLoading(progress -> {
            try {
                // Create a temporary file with the original file extension
                String fileName = fileList.get(index).getName();
                String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
                Path tempFile = Files.createTempFile("blackbox_preview_", extension);
                tempFile.toFile().deleteOnExit(); // Ensure cleanup on JVM exit

                // Extract the file content to the temp file
                try (InputStream in = fileList.get(index).getContentStream();
                     OutputStream out = Files.newOutputStream(tempFile)) {

                    long fileSize = fileList.get(index).getTempFileSize();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        int progressPercent = (int) ((totalRead * 100) / fileSize);
                        progress.accept(progressPercent);
                    }
                }

                // Open the file with the system's default application
                SwingUtilities.invokeLater(() -> {
                    try {
                        Desktop.getDesktop().open(tempFile.toFile());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null,
                                "Could not open file: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (IOException ex) {
                throw new RuntimeException("Failed to open file: " + ex.getMessage());
            }
        }, "Opening file...");
    }

    private static void saveAndClose(JFrame containerFrame) {
        showLoading(progress -> {
            try {
                // Detailed null checks
                if (containerPath == null) {
                    throw new IllegalStateException("Container path is null. Create or open a container first.");
                }
                if (password == null) {
                    throw new IllegalStateException("Password is null. Authentication failed.");
                }

                ContainerManager.saveContainer(containerPath, password, files);
                SwingUtilities.invokeLater(() -> {
                    files.clear();
                    containerPath = null;
                    password = null;
                    containerFrame.dispose(); // Close the window
                });
            } catch (Exception ex) {
                throw new RuntimeException("Save failed: " + ex.getMessage(), ex);
            }
        }, "Saving container...");
    }

    private static JDialog createLoadingDialog(String message) {
        JDialog dialog = new JDialog((Frame) null, "Processing", true);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(message);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        panel.add(label, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        dialog.add(panel);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    private static void showLoading(ProgressTask task, String message) {
        JDialog loadingDialog = createLoadingDialog(message);
        JProgressBar progressBar = (JProgressBar)
                ((JPanel) loadingDialog.getContentPane().getComponent(0)).getComponent(1);

        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run(this::publish);
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latestProgress = chunks.get(chunks.size() - 1);
                progressBar.setValue(latestProgress);
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    get(); // Check for exceptions
                } catch (Exception e) {
                    String errorMessage = "Operation failed: ";
                    if (e.getCause() != null) {
                        errorMessage += e.getCause().getMessage();
                    } else {
                        errorMessage += e.getMessage();
                    }

                    JOptionPane.showMessageDialog(null,
                            errorMessage,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
        loadingDialog.setVisible(true);
    }

    interface ProgressTask {
        void run(Consumer<Integer> progressCallback) throws Exception;
    }
}