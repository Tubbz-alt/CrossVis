package gov.ornl.csed.cda.Talon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static javafx.application.Application.launch;

/**
 * Created by whw on 9/1/16.
 */

/*
    guys.. what we need is a class that can display a single image within a defined boudary

    CONSIDERATIONS
    - also when multiple ones of them are grouped together in the program the desired behavior is that they
        - all display the image at the same size
        - all display the same portion of a single image
        - the image may resize underneath the panel
 */

public class ImageZoomPanel extends JComponent {

    private static Double zoom = 1.0;
    private static Double percentage = 0.01;

    private BufferedImage image = null;

    public ImageZoomPanel(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public ImageZoomPanel() {

    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.scale(zoom, zoom);

        g2.drawImage(image, 0, 0, this);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        repaint();
    }

    public void originalSize() {
        zoom = 1.0;
        repaint();
    }

    public void zoomIn() {
        zoom += percentage;
        repaint();
    }

    public void zoomOut() {
        zoom -= percentage;

        if (zoom < percentage) {
            if (percentage > 1.0) {
                zoom = 1.0;
            } else {
                zoomIn();
            }
        }
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        ImageZoomPanel imageZoomPanel = new ImageZoomPanel();

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem openSingleImage = new JMenuItem("Open Single Image");
        openSingleImage.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            if (fileChooser.showOpenDialog(frame) != JFileChooser.CANCEL_OPTION) {
                File imageFile = fileChooser.getSelectedFile();

                BufferedImage image = null;
                try {
                    image = ImageIO.read(imageFile);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                if (image != null) {
                    imageZoomPanel.setImage(image);
                }
            }
        });

        JMenuItem openMultiImage = new JMenuItem("Open Multiple Images");
        openMultiImage.addActionListener(e -> {

        });

        fileMenu.add(openSingleImage);
        fileMenu.add(openMultiImage);

        menuBar.add(fileMenu);

        JMenu zoomMenu = new JMenu("Zoom");

        JMenuItem zoomIn = new JMenuItem("In");
        zoomIn.addActionListener(e -> {
            imageZoomPanel.zoomIn();
        });

        JMenuItem zoomOut = new JMenuItem("Out");
        zoomOut.addActionListener(e -> {
            imageZoomPanel.zoomOut();
        });

        JMenuItem zoomOriginal = new JMenuItem("Original");
        zoomOriginal.addActionListener(e -> {
            imageZoomPanel.originalSize();
        });

        zoomMenu.add(zoomIn);
        zoomMenu.add(zoomOut);
        zoomMenu.add(zoomOriginal);

        menuBar.add(zoomMenu);

        frame.setJMenuBar(menuBar);

        frame.getContentPane().add(imageZoomPanel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }
}
