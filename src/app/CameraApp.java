package app;

import app.ui.StyledButtonUI;
import app.ui.StyledComboBoxUI;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.TreeMap;


public class CameraApp {

    static {
        // Load OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private JFrame frame;
    private JLabel imageLabel; // Camera preview
    private VideoCapture capture;
    private VideoWriter videoWriter;
    private AtomicBoolean isCameraRunning = new AtomicBoolean(false);
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private Thread cameraThread;

    // Delay options for capture in seconds
    private JComboBox<String> delayComboPhoto;
    private JComboBox<String> delayComboVideo;

    // Format options
    private JComboBox<String> photoFormatCombo;
    private JComboBox<String> videoFormatCombo;

    // For video encoding
    private final int FOURCC_MJPG = VideoWriter.fourcc('M', 'J', 'P', 'G');
    private final int FOURCC_XVID = VideoWriter.fourcc('X', 'V', 'I', 'D');

    // Photo editing variables
    private Mat lastCapturedPhotoMat = null;
    private BufferedImage lastCapturedPhotoImg = null;
    private JLabel editImageLabel;
    private JPanel editPanel;
    private JButton btnRotateLeft, btnRotateRight, btnFlipHorizontal, btnFlipVertical, btnGrayscale, btnResetEdit, btnSaveEdit;

    // Store original captured photo to allow reset
    private Mat originalCapturedMat = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CameraApp().initGUI();
        });
    }

    private void initGUI() {
        frame = new JFrame("Java OpenCV Camera App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setIconImage(new ImageIcon("src/app/assets/images/icon.png").getImage());

        // Camera preview panel
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(new TitledBorder("Camera Preview"));

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setPreferredSize(new Dimension(640, 480));
        previewPanel.add(imageLabel, BorderLayout.CENTER);

        frame.add(previewPanel, BorderLayout.CENTER);

        // CardLayout for controls
        JPanel controlPanel = new JPanel(new CardLayout());
        JPanel cameraControls = createCameraControls();
        JPanel videoControls = createVideoControls();

        controlPanel.add(cameraControls, "Camera");
        controlPanel.add(videoControls, "Video");

        // Switch buttons
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cameraSwitchBtn = new JButton("Camera");
        JButton videoSwitchBtn = new JButton("Video");

        // Set default styles
        cameraSwitchBtn.setBackground(Constants.BUTTON_COLOR);
        cameraSwitchBtn.setForeground(Color.WHITE);
        videoSwitchBtn.setBackground(Color.LIGHT_GRAY);
        videoSwitchBtn.setForeground(Color.BLACK);

        cameraSwitchBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) controlPanel.getLayout();
            cl.show(controlPanel, "Camera");

            // Update button styles
            cameraSwitchBtn.setBackground(Constants.BUTTON_COLOR);
            cameraSwitchBtn.setForeground(Color.WHITE);
            videoSwitchBtn.setBackground(Color.LIGHT_GRAY);
            videoSwitchBtn.setForeground(Color.BLACK);
        });

        videoSwitchBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) controlPanel.getLayout();
            cl.show(controlPanel, "Video");

            // Update button styles
            videoSwitchBtn.setBackground(Constants.BUTTON_COLOR);
            videoSwitchBtn.setForeground(Color.WHITE);
            cameraSwitchBtn.setBackground(Color.LIGHT_GRAY);
            cameraSwitchBtn.setForeground(Color.BLACK);
        });

        switchPanel.add(cameraSwitchBtn);
        switchPanel.add(videoSwitchBtn);
        // Call this method in `initGUI()` after adding the camera/video buttons
        addGalleryButton(switchPanel);

        // Combine switchPanel and controlPanel
        JPanel controlsContainer = new JPanel(new BorderLayout());
        controlsContainer.add(switchPanel, BorderLayout.NORTH);
        controlsContainer.add(controlPanel, BorderLayout.CENTER);

        frame.add(controlsContainer, BorderLayout.SOUTH);

        // Set initial view to "Camera"
        CardLayout cl = (CardLayout) controlPanel.getLayout();
        cl.show(controlPanel, "Camera");

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Setup edit panel
        setupEditPanel();

        initializeCamera();

        applyButtonUI(frame);
        applyComboBoxUI(frame);
    }

    private JPanel createCameraControls() {
        JPanel cameraControls = new JPanel(new GridBagLayout());
        cameraControls.setBorder(new TitledBorder("Camera Controls"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel photoLabel = new JLabel("Photo Capture:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        cameraControls.add(photoLabel, gbc);

        JButton capturePhotoBtn = new JButton("Capture Photo");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cameraControls.add(capturePhotoBtn, gbc);

        delayComboPhoto = new JComboBox<>(new String[]{"0", "1", "2", "3", "5", "10"});
        delayComboPhoto.setToolTipText("Delay before photo capture (seconds)");
        gbc.gridx = 1;
        gbc.gridy = 1;
        cameraControls.add(new JLabel("Delay (s):"), gbc);
        gbc.gridx = 2;
        gbc.gridy = 1;
        cameraControls.add(delayComboPhoto, gbc);

        photoFormatCombo = new JComboBox<>(new String[]{"png", "jpg"});
        photoFormatCombo.setToolTipText("Photo format");
        gbc.gridx = 3;
        gbc.gridy = 1;
        cameraControls.add(new JLabel("Format:"), gbc);
        gbc.gridx = 4;
        gbc.gridy = 1;
        cameraControls.add(photoFormatCombo, gbc);

        capturePhotoBtn.addActionListener(e -> capturePhotoWithDelay());

        return cameraControls;
    }

    private JPanel createVideoControls() {
        JPanel videoControls = new JPanel(new GridBagLayout());
        videoControls.setBorder(new TitledBorder("Video Controls"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel videoLabel = new JLabel("Video Recording:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        videoControls.add(videoLabel, gbc);

        JButton startRecordBtn = new JButton("Start Recording");
        JButton stopRecordBtn = new JButton("Stop Recording");
        stopRecordBtn.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        videoControls.add(startRecordBtn, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        videoControls.add(stopRecordBtn, gbc);

        delayComboVideo = new JComboBox<>(new String[]{"0", "1", "2", "3", "5", "10"});
        delayComboVideo.setToolTipText("Delay before video recording start (seconds)");
        gbc.gridx = 2;
        gbc.gridy = 1;
        videoControls.add(new JLabel("Delay (s):"), gbc);
        gbc.gridx = 3;
        gbc.gridy = 1;
        videoControls.add(delayComboVideo, gbc);

        videoFormatCombo = new JComboBox<>(new String[]{"avi", "mp4"});
        videoFormatCombo.setToolTipText("Video format");
        gbc.gridx = 4;
        gbc.gridy = 1;
        videoControls.add(new JLabel("Format:"), gbc);
        gbc.gridx = 5;
        gbc.gridy = 1;
        videoControls.add(videoFormatCombo, gbc);

        startRecordBtn.addActionListener(e -> {
            startRecordBtn.setEnabled(false);
            stopRecordBtn.setEnabled(true);
            startVideoRecordingWithDelay();
        });

        stopRecordBtn.addActionListener(e -> {
            stopRecordBtn.setEnabled(false);
            startRecordBtn.setEnabled(true);
            stopVideoRecording();
        });

        return videoControls;
    }

    private void applyButtonUI(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                ((JButton) component).setUI(new StyledButtonUI());
                ((JButton) component).setBackground(Constants.BUTTON_COLOR);
                ((JButton) component).setForeground(Color.WHITE);
                ((JButton) component).setFocusPainted(false);
                ((JButton) component).setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
            } else if (component instanceof Container) {
                applyButtonUI((Container) component);
            }
        }
    }

    private void applyComboBoxUI(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                ((JComboBox<?>) component).setUI(new StyledComboBoxUI());
            } else if (component instanceof Container) {
                applyComboBoxUI((Container) component); // Recursively apply to child containers
            }
        }
    }

    private void setupEditPanel() {
        editPanel = new JPanel(new BorderLayout());
        editPanel.setBorder(new TitledBorder("Photo Editing"));
        editPanel.setPreferredSize(new Dimension(640, 250));

        editImageLabel = new JLabel();
        editImageLabel.setHorizontalAlignment(JLabel.CENTER);
        editImageLabel.setPreferredSize(new Dimension(640, 200));
        editPanel.add(editImageLabel, BorderLayout.CENTER);

        // Rotate category
        JPanel rotatePanel = new JPanel();
        rotatePanel.setLayout(new BoxLayout(rotatePanel, BoxLayout.Y_AXIS));
        rotatePanel.setBorder(new TitledBorder("Rotate"));
        btnRotateLeft = new JButton("Rotate Left");
        btnRotateRight = new JButton("Rotate Right");
        rotatePanel.add(btnRotateLeft);
        rotatePanel.add(Box.createVerticalStrut(5)); // Add spacing
        rotatePanel.add(btnRotateRight);

        // Flip category
        JPanel flipPanel = new JPanel();
        flipPanel.setLayout(new BoxLayout(flipPanel, BoxLayout.Y_AXIS));
        flipPanel.setBorder(new TitledBorder("Flip"));
        btnFlipHorizontal = new JButton("Flip Horizontal");
        btnFlipVertical = new JButton("Flip Vertical");
        flipPanel.add(btnFlipHorizontal);
        flipPanel.add(Box.createVerticalStrut(5)); // Add spacing
        flipPanel.add(btnFlipVertical);

        // Filter category
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder("Filter"));
        btnGrayscale = new JButton("Grayscale");
        btnResetEdit = new JButton("Reset");
        filterPanel.add(btnGrayscale);
        filterPanel.add(Box.createVerticalStrut(5)); // Add spacing
        filterPanel.add(btnResetEdit);

        // Save button
        JPanel savePanel = new JPanel();
        savePanel.setLayout(new BoxLayout(savePanel, BoxLayout.Y_AXIS));
        btnSaveEdit = new JButton("Save Edited Photo");
        savePanel.add(btnSaveEdit);

        // Combine all panels horizontally
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        buttonsPanel.add(rotatePanel);
        buttonsPanel.add(flipPanel);
        buttonsPanel.add(filterPanel);
        buttonsPanel.add(savePanel);

        editPanel.add(buttonsPanel, BorderLayout.SOUTH);
        editPanel.setVisible(false);

        frame.add(editPanel, BorderLayout.NORTH);

        // Button actions
        btnRotateLeft.addActionListener(e -> applyRotation(-90));
        btnRotateRight.addActionListener(e -> applyRotation(90));
        btnFlipHorizontal.addActionListener(e -> applyFlip(1));
        btnFlipVertical.addActionListener(e -> applyFlip(0));
        btnGrayscale.addActionListener(e -> applyGrayscale());
        btnResetEdit.addActionListener(e -> resetEdit());
        btnSaveEdit.addActionListener(e -> {
            saveEditedPhotoToPictures();
            SwingUtilities.getWindowAncestor(editPanel).dispose(); // Close the edit window
            isCameraRunning.set(true); // Restart the camera
            initializeCamera(); // Reinitialize the camera
        });
    }

    private void saveEditedPhotoToPictures() {
        if (lastCapturedPhotoMat == null) {
            JOptionPane.showMessageDialog(frame, "No photo to save", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File picturesFolder = new File(System.getProperty("user.home") + "/Pictures/CameraApp/images");
        if (!picturesFolder.exists()) {
            picturesFolder.mkdirs();
        }

        String selectedFormat = (String) photoFormatCombo.getSelectedItem();
        String fileName = "edited_photo_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "." + selectedFormat;
        File fileToSave = new File(picturesFolder, fileName);

        // Save the image directly without unnecessary color conversion
        boolean result = Imgcodecs.imwrite(fileToSave.getAbsolutePath(), lastCapturedPhotoMat);

        if (result) {
            JOptionPane.showMessageDialog(frame, "Edited photo saved to: " + fileToSave.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "Failed to save photo", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeCamera() {
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(frame, "Cannot open webcam", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        isCameraRunning.set(true);

        cameraThread = new Thread(() -> {
            Mat frameMat = new Mat();
            while (isCameraRunning.get()) {
                if (capture.read(frameMat)) {
                    Image image = matToBufferedImage(frameMat);
                    if (image != null) {
                        ImageIcon icon = new ImageIcon(image);
                        SwingUtilities.invokeLater(() -> imageLabel.setIcon(icon));
                    }
                } else {
                    System.err.println("Failed to read frame from camera");
                }
                try {
                    Thread.sleep(33); // roughly 30 FPS
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            frameMat.release();
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void capturePhotoWithDelay() {
        int delaySec = Integer.parseInt((String) delayComboPhoto.getSelectedItem());

        if (delaySec > 0) {
            JOptionPane.showMessageDialog(frame, "Photo will be captured in " + delaySec + " seconds.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }

        new Thread(() -> {
            try {
                Thread.sleep(delaySec * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            capturePhotoOriginalSaving();
        }).start();
    }

    private void capturePhotoOriginalSaving() {
        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(frame, "Camera is not opened", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Mat frameMat = new Mat();
        if (capture.read(frameMat)) {
            if (originalCapturedMat != null) {
                originalCapturedMat.release();
            }
            originalCapturedMat = frameMat.clone();

            if (lastCapturedPhotoMat != null) {
                lastCapturedPhotoMat.release();
            }
            lastCapturedPhotoMat = frameMat.clone();

            lastCapturedPhotoImg = matToBufferedImage(lastCapturedPhotoMat);
            updateEditImageLabel(lastCapturedPhotoImg);

            showEditPanel(true);

            JOptionPane.showMessageDialog(frame, "Photo captured! Use editing tools below.", "Info", JOptionPane.INFORMATION_MESSAGE);

            frameMat.release();
        } else {
            JOptionPane.showMessageDialog(frame, "Failed to capture photo", "Error", JOptionPane.ERROR_MESSAGE);
            frameMat.release();
        }
    }

    private void showEditPanel(boolean visible) {
        if (visible) {
            // Pause the camera
            isCameraRunning.set(false);

            // Create a new JFrame for the edit panel
            JFrame editFrame = new JFrame("Photo Editing");
            editFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editFrame.setLayout(new BorderLayout());
            editFrame.setSize(700, 600);
            editFrame.setLocationRelativeTo(frame);

            // Ensure the edit panel is visible and add it to the new frame
            editPanel.setVisible(true);
            editFrame.add(editPanel, BorderLayout.CENTER);

            // Add a listener to resume the camera when the edit window is closed
            editFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    isCameraRunning.set(true);
                    initializeCamera(); // Resume the camera
                }
            });

            editFrame.setVisible(true);
        }
    }

    private void updateEditImageLabel(BufferedImage img) {
        if (img != null) {
            ImageIcon icon = new ImageIcon(img.getScaledInstance(editImageLabel.getWidth(), editImageLabel.getHeight(), Image.SCALE_SMOOTH));
            editImageLabel.setIcon(icon);
        }
    }

    // Apply rotation in degrees (-90 for left, 90 for right)
    private void applyRotation(int degrees) {
        if (lastCapturedPhotoMat == null) return;

        Mat rotated = new Mat();
        int rotateCode;
        if (degrees == 90) {
            rotateCode = Core.ROTATE_90_CLOCKWISE;
        } else if (degrees == -90) {
            rotateCode = Core.ROTATE_90_COUNTERCLOCKWISE;
        } else {
            return; // Unsupported
        }
        Core.rotate(lastCapturedPhotoMat, rotated, rotateCode);
        lastCapturedPhotoMat.release();
        lastCapturedPhotoMat = rotated;
        lastCapturedPhotoImg = matToBufferedImage(lastCapturedPhotoMat);
        updateEditImageLabel(lastCapturedPhotoImg);
    }

    // flipCode: 0 = x-axis vertical flip, 1 = y-axis horizontal flip
    private void applyFlip(int flipCode) {
        if (lastCapturedPhotoMat == null) return;

        Mat flipped = new Mat();
        Core.flip(lastCapturedPhotoMat, flipped, flipCode);
        lastCapturedPhotoMat.release();
        lastCapturedPhotoMat = flipped;
        lastCapturedPhotoImg = matToBufferedImage(lastCapturedPhotoMat);
        updateEditImageLabel(lastCapturedPhotoImg);
    }

    private void applyGrayscale() {
        if (lastCapturedPhotoMat == null) return;

        Mat gray = new Mat();
        Imgproc.cvtColor(lastCapturedPhotoMat, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(gray, lastCapturedPhotoMat, Imgproc.COLOR_GRAY2RGB);
        gray.release();

        lastCapturedPhotoImg = matToBufferedImage(lastCapturedPhotoMat);
        updateEditImageLabel(lastCapturedPhotoImg);
    }

    private void resetEdit() {
        if (originalCapturedMat == null) return;

        if (lastCapturedPhotoMat != null) {
            lastCapturedPhotoMat.release();
        }
        lastCapturedPhotoMat = originalCapturedMat.clone();
        lastCapturedPhotoImg = matToBufferedImage(lastCapturedPhotoMat);
        updateEditImageLabel(lastCapturedPhotoImg);
    }

    private void saveEditedPhoto() {
        if (lastCapturedPhotoMat == null) {
            JOptionPane.showMessageDialog(frame, "No photo to save", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Edited Photo");
        String selectedFormat = (String) photoFormatCombo.getSelectedItem();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(selectedFormat.toUpperCase() + " Images", selectedFormat);
        fileChooser.setFileFilter(filter);

        String defaultFileName = "edited_photo_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "." + selectedFormat;
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith("." + selectedFormat)) {
                filePath += "." + selectedFormat;
            }

            Mat saveMat = new Mat();
            Imgproc.cvtColor(lastCapturedPhotoMat, saveMat, Imgproc.COLOR_RGB2BGR);

            boolean result = Imgcodecs.imwrite(filePath, saveMat);
            saveMat.release();

            if (result) {
                JOptionPane.showMessageDialog(frame, "Edited photo saved: " + filePath, "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to save photo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startVideoRecordingWithDelay() {
        int delaySec = Integer.parseInt((String) delayComboVideo.getSelectedItem());

        if (delaySec > 0) {
            JOptionPane.showMessageDialog(frame, "Video recording will start in " + delaySec + " seconds.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }

        new Thread(() -> {
            try {
                Thread.sleep(delaySec * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            startVideoRecording();
        }).start();
    }

    private JLabel recordingProgressLabel;

    private void startVideoRecording() {
        if (isRecording.get()) {
            JOptionPane.showMessageDialog(frame, "Already recording", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(frame, "Camera is not opened", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File videosFolder = new File(System.getProperty("user.home") + "/Pictures/CameraApp/videos");
        if (!videosFolder.exists()) {
            videosFolder.mkdirs();
        }

        String selectedFormat = (String) videoFormatCombo.getSelectedItem();
        String fileName = "video_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "." + selectedFormat;
        File fileToSave = new File(videosFolder, fileName);

        String filePath = fileToSave.getAbsolutePath();

        int fourcc;
        if ("avi".equals(selectedFormat)) {
            fourcc = FOURCC_MJPG;
        } else if ("mp4".equals(selectedFormat)) {
            fourcc = VideoWriter.fourcc('H', '2', '6', '4'); // May work if supported by codec
        } else {
            fourcc = FOURCC_MJPG;
        }

        double fps = 20.0;
        int frameWidth = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frameHeight = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);

        videoWriter = new VideoWriter(filePath, fourcc, fps, new Size(frameWidth, frameHeight));
        if (!videoWriter.isOpened()) {
            JOptionPane.showMessageDialog(frame, "Failed to open video file for writing", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        isRecording.set(true);

        // Initialize recording progress label
        recordingProgressLabel = new JLabel("Recording: 0s");
        frame.add(recordingProgressLabel, BorderLayout.NORTH);
        frame.revalidate();

        long startTime = System.currentTimeMillis();

        Thread recordThread = new Thread(() -> {
            Mat frameMat = new Mat();
            while (isRecording.get()) {
                if (capture.read(frameMat)) {
                    Mat bgrFrame = frameMat.clone();
                    videoWriter.write(bgrFrame);
                    bgrFrame.release();
                } else {
                    System.err.println("Failed to read frame during video recording");
                }

                // Update recording progress
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                SwingUtilities.invokeLater(() -> recordingProgressLabel.setText("Recording: " + elapsedTime + "s"));

                try {
                    Thread.sleep((long) (1000.0 / fps));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            frameMat.release();
            videoWriter.release();

            // Remove progress label after recording stops
            SwingUtilities.invokeLater(() -> {
                frame.remove(recordingProgressLabel);
                frame.revalidate();
                frame.repaint();
            });
        });
        recordThread.setDaemon(true);
        recordThread.start();

        JOptionPane.showMessageDialog(frame, "Recording started", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void stopVideoRecording() {
        if (!isRecording.get()) {
            JOptionPane.showMessageDialog(frame, "Not currently recording", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        isRecording.set(false);
        JOptionPane.showMessageDialog(frame, "Recording stopped", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void releaseResources() {
        isCameraRunning.set(false);

        try {
            if (cameraThread != null) {
                cameraThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        if (videoWriter != null && videoWriter.isOpened()) {
            videoWriter.release();
        }
        if (lastCapturedPhotoMat != null) {
            lastCapturedPhotoMat.release();
        }
        if (originalCapturedMat != null) {
            originalCapturedMat.release();
        }
        System.out.println("Resources released, exiting.");
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) return null;

        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (mat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] pixels = new byte[bufferSize];
        mat.get(0, 0, pixels);

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(pixels, 0, targetPixels, 0, pixels.length);

        return image;
    }

    private void addGalleryButton(JPanel switchPanel) {
        JButton galleryButton = new JButton("Gallery");
        galleryButton.setBackground(Color.GRAY);
        galleryButton.setForeground(Color.WHITE);
        galleryButton.setFocusPainted(false);
        galleryButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        galleryButton.addActionListener(e -> openGalleryWindow());

        switchPanel.add(Box.createHorizontalStrut(20)); // Add spacing
        switchPanel.add(galleryButton);
    }

    private void openGalleryWindow() {
        JFrame galleryFrame = new JFrame("Gallery");
        galleryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        galleryFrame.setLayout(new BorderLayout());
        galleryFrame.setSize(800, 600);
        galleryFrame.setLocationRelativeTo(frame);

        JPanel galleryPanel = new JPanel();
        galleryPanel.setLayout(new BoxLayout(galleryPanel, BoxLayout.Y_AXIS)); // Group items vertically
        galleryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        File imagesFolder = new File(System.getProperty("user.home") + "/Pictures/CameraApp/images");
        File videosFolder = new File(System.getProperty("user.home") + "/Pictures/CameraApp/videos");

        Map<String, JPanel> dateGroups = new TreeMap<>(); // Sorted by date

        addFilesToDateGroups(imagesFolder, dateGroups, "Photo");
        addFilesToDateGroups(videosFolder, dateGroups, "Video");

        for (Map.Entry<String, JPanel> entry : dateGroups.entrySet()) {
            JPanel datePanel = new JPanel(new BorderLayout());
            datePanel.setBorder(BorderFactory.createTitledBorder(entry.getKey()));
            datePanel.add(entry.getValue(), BorderLayout.CENTER);
            galleryPanel.add(datePanel);
        }

        JScrollPane scrollPane = new JScrollPane(galleryPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        galleryFrame.add(scrollPane, BorderLayout.CENTER);
        galleryFrame.setVisible(true);
    }

    private void addFilesToDateGroups(File folder, Map<String, JPanel> dateGroups, String itemType) {
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(file.lastModified()));
                    dateGroups.putIfAbsent(date, new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)));

                    JLabel itemLabel = new JLabel();
                    if ("Photo".equals(itemType)) {
                        ImageIcon imageIcon = new ImageIcon(file.getAbsolutePath());
                        itemLabel.setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH)));
                    } else {
                        itemLabel.setIcon(new ImageIcon("src/app/assets/icons/video_placeholder.png")); // Placeholder for video
                    }
                    itemLabel.setPreferredSize(new Dimension(200, 200));
                    dateGroups.get(date).add(itemLabel);
                }
            }
        }
    }

    private void groupItemsByDate(File folder, JPanel galleryPanel, String itemType) {
        Map<String, JPanel> dateGroups = new TreeMap<>(); // Sorted by date

        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(file.lastModified()));
                dateGroups.putIfAbsent(date, new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)));

                JLabel itemLabel = new JLabel();
                if ("Photo".equals(itemType)) {
                    ImageIcon imageIcon = new ImageIcon(file.getAbsolutePath());
                    itemLabel.setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH)));
                } else {
                    itemLabel.setIcon(new ImageIcon("src/app/assets/icons/video_placeholder.png")); // Placeholder for video
                }
                itemLabel.setPreferredSize(new Dimension(200, 200));
                dateGroups.get(date).add(itemLabel);
            }
        }

        for (Map.Entry<String, JPanel> entry : dateGroups.entrySet()) {
            JPanel datePanel = new JPanel(new BorderLayout());
            datePanel.setBorder(BorderFactory.createTitledBorder(entry.getKey()));
            datePanel.add(entry.getValue(), BorderLayout.CENTER);
            galleryPanel.add(datePanel);
        }
    }

    private void setupGalleryPanel() {
        JPanel galleryPanel = new JPanel(new BorderLayout());
        galleryPanel.setBorder(new TitledBorder("Gallery"));
        galleryPanel.setPreferredSize(new Dimension(640, 480));

        JList<String> fileList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(fileList);
        galleryPanel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            DefaultListModel<String> listModel = new DefaultListModel<>();
            File imagesFolder = new File(System.getProperty("user.home") + "/Pictures/CameraApp/images");
            File videosFolder = new File(System.getProperty("user.home") + "/Pictures/CameraApp/videos");

            if (imagesFolder.exists()) {
                for (File file : imagesFolder.listFiles()) {
                    listModel.addElement("Photo: " + file.getName());
                }
            }
            if (videosFolder.exists()) {
                for (File file : videosFolder.listFiles()) {
                    listModel.addElement("Video: " + file.getName());
                }
            }
            fileList.setModel(listModel);
        });

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedItem = fileList.getSelectedValue();
                    if (selectedItem != null) {
                        String filePath = selectedItem.startsWith("Photo: ")
                                ? System.getProperty("user.home") + "/Pictures/CameraApp/images/" + selectedItem.substring(7)
                                : System.getProperty("user.home") + "/Pictures/CameraApp/videos/" + selectedItem.substring(7);
                        try {
                            Desktop.getDesktop().open(new File(filePath));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame, "Failed to open file: " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        galleryPanel.add(refreshButton, BorderLayout.SOUTH);
        frame.add(galleryPanel, BorderLayout.EAST);
    }
}

