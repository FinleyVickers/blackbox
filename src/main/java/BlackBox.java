// BlackBox.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class BlackBox {
    private static Map<String, StoredFile> files = new HashMap<>();
    private static String containerPath;
    private static String password;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            System.err.println("GTK look and feel not available");
        }

        SwingUtilities.invokeLater(BlackBox::createAndShowMainUI);
    }

    private static void createAndShowMainUI() {
        JFrame frame = new JFrame("BlackBox - Secure Storage");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

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
        }
    }

    private static void openExistingContainer() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select container file");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

        containerPath = chooser.getSelectedFile().getAbsolutePath();
        password = getPasswordFromDialog("Enter password:");

        if (password != null) {
            try {
                files = ContainerManager.loadContainer(containerPath, password);
                JOptionPane.showMessageDialog(null, "Container unlocked successfully!");
                showContainerUI();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
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
        JFrame containerFrame = new JFrame("Container Management");
        containerFrame.setSize(500, 300);
        containerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton addBtn = new JButton("Add Files");
        JButton listBtn = new JButton("View Stored Files");
        JButton extractBtn = new JButton("Extract File");
        JButton saveBtn = new JButton("Save and Close");

        addBtn.addActionListener(e -> addFiles());
        listBtn.addActionListener(e -> listFiles());
        extractBtn.addActionListener(e -> extractFile());
        saveBtn.addActionListener(e -> {
            saveAndClose();
            containerFrame.dispose();
        });

        panel.add(addBtn);
        panel.add(listBtn);
        panel.add(extractBtn);
        panel.add(saveBtn);

        containerFrame.add(panel);
        containerFrame.setLocationRelativeTo(null);
        containerFrame.setVisible(true);
    }

    private static void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select files to encrypt");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

        for (File file : chooser.getSelectedFiles()) {
            try {
                Path path = file.toPath();
                String fileName = path.getFileName().toString();
                String fileType = Files.probeContentType(path) != null ?
                        Files.probeContentType(path) : "unknown";
                byte[] content = Files.readAllBytes(path);

                files.put(fileName, new StoredFile(fileName, fileType, content));
                JOptionPane.showMessageDialog(null, "Added: " + fileName);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error adding " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        saveContainer();
    }

    private static void listFiles() {
        JFrame listFrame = new JFrame("Stored Files");
        listFrame.setSize(400, 300);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (StoredFile file : files.values()) {
            listModel.addElement(file.name() + " (" + file.type() + ")");
        }

        JList<String> fileList = new JList<>(listModel);
        listFrame.add(new JScrollPane(fileList));
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
                .map(f -> f.name() + " (" + f.type() + ")")
                .toArray(String[]::new);

        String selection = (String) JOptionPane.showInputDialog(
                null, "Select file to extract:", "Extract File",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]
        );

        if (selection == null) return;
        int index = Arrays.asList(options).indexOf(selection);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select save location");
        chooser.setSelectedFile(new File(fileList.get(index).name()));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(chooser.getSelectedFile().toPath(), fileList.get(index).content());
                JOptionPane.showMessageDialog(null, "File extracted successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(null, "Container saved successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}