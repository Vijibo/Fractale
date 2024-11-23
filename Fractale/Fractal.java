import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Fractal extends JFrame {

    private double minX = -2.0, maxX = 1.0;
    private double minY = -1.5, maxY = 1.5;
    private final double initialMinX = -2.0, initialMaxX = 1.0;
    private final double initialMinY = -1.5, initialMaxY = 1.5;
    private BufferedImage fractalImage;
    private boolean isRendering = false;

    private int lastMouseX, lastMouseY;

    // JLabel pour afficher les coordonnées
    private JLabel coordinatesLabel;

    // Liste déroulante pour le choix des couleurs
    private JComboBox<String> colorSchemeSelector;
    private int selectedColorScheme = 0;

    public Fractal() {
        setTitle("Fractal Viewer");
        setSize(800, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        FractalPanel fractalPanel = new FractalPanel();
        fractalImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        generateFractal();

        // Créez le JLabel pour afficher les coordonnées
        coordinatesLabel = new JLabel("Mouse Coordinates: ");
        coordinatesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coordinatesLabel.setFont(new Font("Arial", Font.BOLD, 14));
        coordinatesLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Liste déroulante pour changer les couleurs
        String[] colorSchemes = {"Default", "Blue Gradient", "Warm Tones", "Grayscale"};
        colorSchemeSelector = new JComboBox<>(colorSchemes);
        colorSchemeSelector.addActionListener(e -> {
            selectedColorScheme = colorSchemeSelector.getSelectedIndex();
            generateFractal();
            fractalPanel.repaint();
        });

        // Panneau principal contenant tout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(coordinatesLabel, BorderLayout.NORTH);
        mainPanel.add(fractalPanel, BorderLayout.CENTER);

        // Panneau inférieur pour les boutons
        JPanel controlsPanel = new JPanel();
        JButton resetButton = new JButton("Reset");
        JButton captureButton = new JButton("Capture");
        controlsPanel.add(new JLabel("Theme : "));
        controlsPanel.add(colorSchemeSelector);
        controlsPanel.add(resetButton);
        controlsPanel.add(captureButton);
        mainPanel.add(controlsPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Listener pour le bouton "Reset"
        resetButton.addActionListener(e -> {
            resetFractal();
            fractalPanel.repaint();
        });

        // Listener pour le bouton "Capture"
        captureButton.addActionListener(e -> saveFractalImage());

        addMouseWheelListener(e -> {
            if (!isRendering) {
                isRendering = true;
                SwingUtilities.invokeLater(() -> {
                    if (e.getWheelRotation() < 0) {
                        zoom(0.5, e.getX(), e.getY());
                    } else {
                        zoom(2.0, e.getX(), e.getY());
                    }
                    fractalPanel.repaint();
                    isRendering = false;
                });
            }
        });

        fractalPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        fractalPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int deltaX = e.getX() - lastMouseX;
                int deltaY = e.getY() - lastMouseY;

                double scaleX = (maxX - minX) / fractalImage.getWidth();
                double scaleY = (maxY - minY) / fractalImage.getHeight();
                double deltaFracX = deltaX * scaleX;
                double deltaFracY = deltaY * scaleY;

                minX -= deltaFracX;
                maxX -= deltaFracX;
                minY -= deltaFracY;
                maxY -= deltaFracY;

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                generateFractal();
                fractalPanel.repaint();
            }

            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                double coordX = minX + e.getX() * (maxX - minX) / fractalImage.getWidth();
                double coordY = minY + e.getY() * (maxY - minY) / fractalImage.getHeight();
                coordinatesLabel.setText(String.format("Mouse Coordinates: (%.5f, %.5f)", coordX, coordY));
            }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int newWidth = getWidth();
                int newHeight = getHeight();

                double aspectRatio = (double) newWidth / newHeight;

                double currentWidth = maxX - minX;
                double newHeightAdjusted = currentWidth / aspectRatio;

                double centerY = (minY + maxY) / 2;
                minY = centerY - newHeightAdjusted / 2;
                maxY = centerY + newHeightAdjusted / 2;

                fractalImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                generateFractal();
                fractalPanel.repaint();
            }
        });

        setVisible(true);
    }

    private void generateFractal() {
        int width = fractalImage.getWidth();
        int height = fractalImage.getHeight();

        int numThreads = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[numThreads];

        for (int t = 0; t < numThreads; t++) {
            int startY = t * height / numThreads;
            int endY = (t + 1) * height / numThreads;

            threads[t] = new Thread(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        double c_re = minX + x * (maxX - minX) / width;
                        double c_im = minY + y * (maxY - minY) / height;

                        int iterations = mandelbrot(c_re, c_im);

                        int color = getColor(iterations);
                        fractalImage.setRGB(x, y, color);
                    }
                }
            });
            threads[t].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int mandelbrot(double c_re, double c_im) {
        double z_re = 0, z_im = 0;
        int maxIterations = 1000;

        for (int i = 0; i < maxIterations; i++) {
            double z_re2 = z_re * z_re - z_im * z_im + c_re;
            double z_im2 = 2.0 * z_re * z_im + c_im;

            z_re = z_re2;
            z_im = z_im2;

            if (z_re * z_re + z_im * z_im > 4.0) {
                return i;
            }
        }
        return maxIterations;
    }

    private int getColor(int iterations) {
        if (iterations == 1000) {
            return Color.HSBtoRGB(0, 0, 0); // Noir pour les points de la fractale
        }
        switch (selectedColorScheme) {
            case 1: // Blue Gradient
                return Color.HSBtoRGB(0.6f, (float) iterations / 1000, 1);
            case 2: // Warm Tones
                return Color.HSBtoRGB((float) iterations / 1000, 1, 1);
            case 3: // Grayscale
                return Color.HSBtoRGB(0, 0, (float) iterations / 1000);
            default: // Default
                return Color.HSBtoRGB((float) iterations / 1000, 1, (float) Math.sqrt((double) iterations / 1000));
        }
    }

    private void saveFractalImage() {
        try {
            String[] formats = {"png", "jpeg", "bmp"};
            String format = (String) JOptionPane.showInputDialog(
                    this,
                    "Choose a format:",
                    "Save Fractal",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    formats,
                    formats[0]
            );

            if (format != null) {
                File outputFile = new File("fractal." + format);
                ImageIO.write(fractalImage, format, outputFile);
                JOptionPane.showMessageDialog(this, "Fractal saved as fractal." + format);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save capture!");
        }
    }

    private void zoom(double zoomFactor, int mouseX, int mouseY) {
        double centerX = minX + mouseX * (maxX - minX) / fractalImage.getWidth();
        double centerY = minY + mouseY * (maxY - minY) / fractalImage.getHeight();

        double rangeX = (maxX - minX) * zoomFactor;
        double rangeY = (maxY - minY) * zoomFactor;

        minX = centerX - rangeX / 2;
        maxX = centerX + rangeX / 2;
        minY = centerY - rangeY / 2;
        maxY = centerY + rangeY / 2;

        generateFractal();
    }

    private void resetFractal() {
        minX = initialMinX;
        maxX = initialMaxX;
        minY = initialMinY;
        maxY = initialMaxY;
        generateFractal();
    }

    private class FractalPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(fractalImage, 0, 0, null);
        }
    }

    public static void main(String[] args) {
        new Fractal();
    }
}
