import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
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
    private static List<File> pendingFiles = new ArrayList<>(); // Track files waiting to be added
    private static DefaultListModel<String> pendingListModel; // Model for the pending files list
    private static JPanel dropPanel; // Make dropPanel accessible
    private static JPanel confirmPanel; // Panel for confirmation button

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

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create drop target panel with card layout to switch between drop zone and file list
        dropPanel = new JPanel(new CardLayout());
        
        // Create the initial drop zone
        JPanel dropZone = new JPanel(new BorderLayout());
        dropZone.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 5, 5));
        JLabel dropLabel = new JLabel("Drop files here", SwingConstants.CENTER);
        dropLabel.setFont(new Font(dropLabel.getFont().getName(), Font.PLAIN, 16));
        dropZone.add(dropLabel, BorderLayout.CENTER);

        // Create the pending files list panel
        JPanel listPanel = new JPanel(new BorderLayout());
        pendingListModel = new DefaultListModel<>();
        JList<String> pendingList = new JList<>(pendingListModel);
        JScrollPane scrollPane = new JScrollPane(pendingList);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        // Create confirmation panel
        confirmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton confirmBtn = new JButton("Save Files to Container");
        JButton cancelBtn = new JButton("Cancel");
        confirmPanel.add(confirmBtn);
        confirmPanel.add(cancelBtn);
        confirmPanel.setVisible(false);
        
        // Add both panels to the card layout
        dropPanel.add(dropZone, "DROP_ZONE");
        dropPanel.add(listPanel, "FILE_LIST");

        // Add drag and drop support to both panels
        DropTargetAdapter dropAdapter = new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    if (dropZone.isShowing()) {
                        dropZone.setBorder(BorderFactory.createDashedBorder(Color.BLUE, 5, 5));
                    }
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                if (dropZone.isShowing()) {
                    dropZone.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 5, 5));
                }
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transferData = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (transferData instanceof List<?> && ((List<?>) transferData).stream().allMatch(item -> item instanceof File)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) transferData;
                        addToPendingFiles(droppedFiles);
                        if (dropZone.isShowing()) {
                            dropZone.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 5, 5));
                        }
                    } else {
                        throw new UnsupportedFlavorException(DataFlavor.javaFileListFlavor);
                    }
                } catch (Exception e) {
                    dtde.rejectDrop();
                    JOptionPane.showMessageDialog(activeContainerFrame,
                            "Error processing dropped files: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        new DropTarget(dropZone, dropAdapter);
        new DropTarget(listPanel, dropAdapter);

        // Add action listeners for confirm/cancel buttons
        confirmBtn.addActionListener(e -> {
            handleDroppedFiles(pendingFiles.toArray(new File[0]));
            pendingFiles.clear();
            pendingListModel.clear();
            ((CardLayout)dropPanel.getLayout()).show(dropPanel, "DROP_ZONE");
            confirmPanel.setVisible(false);
        });

        cancelBtn.addActionListener(e -> {
            pendingFiles.clear();
            pendingListModel.clear();
            ((CardLayout)dropPanel.getLayout()).show(dropPanel, "DROP_ZONE");
            confirmPanel.setVisible(false);
        });

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 10, 10));
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

        buttonPanel.add(addBtn);
        buttonPanel.add(listBtn);
        buttonPanel.add(openBtn);
        buttonPanel.add(extractBtn);
        buttonPanel.add(saveBtn);

        mainPanel.add(dropPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.EAST);
        mainPanel.add(confirmPanel, BorderLayout.SOUTH);

        activeContainerFrame.add(mainPanel);
        activeContainerFrame.setLocationRelativeTo(null);
        activeContainerFrame.setVisible(true);
    }

    private static void addToPendingFiles(List<File> newFiles) {
        for (File file : newFiles) {
            if (!pendingFiles.contains(file)) {
                pendingFiles.add(file);
                pendingListModel.addElement(file.getName());
            }
        }
        if (!pendingFiles.isEmpty()) {
            ((CardLayout)dropPanel.getLayout()).show(dropPanel, "FILE_LIST");
            confirmPanel.setVisible(true);
        }
    }

    private static void handleDroppedFiles(File[] droppedFiles) {
        showLoading(progress -> {
            for (int i = 0; i < droppedFiles.length; i++) {
                File file = droppedFiles[i];
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
                                    p -> progress.accept((finalI * 100 + p) / droppedFiles.length)
                            )
                    );
                    ContainerManager.saveContainer(containerPath, password, files);
                } catch (Exception e) {
                    throw new RuntimeException("Error adding file: " + e.getMessage());
                }
            }
        }, "Encrypting files...");
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