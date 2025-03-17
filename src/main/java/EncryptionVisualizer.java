import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class EncryptionVisualizer extends JPanel {
    private static final int ROWS = 8;
    private static final int COLS = 32;
    private static final int BIT_SIZE = 10;
    private final List<Point> encryptedBits = new ArrayList<>();
    private final Random random = new Random();
    private int progress = 0;
    private String currentFileName = "";

    public EncryptionVisualizer() {
        setPreferredSize(new Dimension(COLS * BIT_SIZE, ROWS * BIT_SIZE + 30));
        setBackground(Color.BLACK);
    }

    public void startAnimation(String fileName) {
        currentFileName = fileName;
        encryptedBits.clear();
        progress = 0;
        repaint();
    }

    public void updateProgress(int newProgress) {
        this.progress = newProgress;
        int totalBits = ROWS * COLS;
        int targetBitCount = (progress * totalBits) / 100;
        
        // Add new bits to match the progress
        while (encryptedBits.size() < targetBitCount) {
            Point newBit;
            do {
                newBit = new Point(random.nextInt(COLS), random.nextInt(ROWS));
            } while (encryptedBits.contains(newBit));
            encryptedBits.add(newBit);
        }
        
        repaint();
    }

    public void stopAnimation() {
        // Fill in any remaining bits instantly
        int totalBits = ROWS * COLS;
        while (encryptedBits.size() < totalBits) {
            Point newBit;
            do {
                newBit = new Point(random.nextInt(COLS), random.nextInt(ROWS));
            } while (encryptedBits.contains(newBit));
            encryptedBits.add(newBit);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the grid of bits
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Point bit = new Point(col, row);
                if (encryptedBits.contains(bit)) {
                    g2d.setColor(Color.GREEN);
                    g2d.fillRect(col * BIT_SIZE, row * BIT_SIZE, BIT_SIZE - 1, BIT_SIZE - 1);
                } else {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawRect(col * BIT_SIZE, row * BIT_SIZE, BIT_SIZE - 1, BIT_SIZE - 1);
                }
            }
        }

        // Draw progress text and filename
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospace", Font.PLAIN, 12));
        String progressText = String.format("Encrypting: %d%%", progress);
        g2d.drawString(progressText, 10, ROWS * BIT_SIZE + 20);
        g2d.drawString(currentFileName, getWidth() - g2d.getFontMetrics().stringWidth(currentFileName) - 10, 
                      ROWS * BIT_SIZE + 20);
    }
} 