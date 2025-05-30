package app;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class CameraHandler {
    private VideoCapture camera;

    public CameraHandler() {
        camera = new VideoCapture(0); // 0 for default camera
    }

    public boolean isCameraOpen() {
        return camera.isOpened();
    }

    public BufferedImage captureFrame() {
        Mat frame = new Mat();
        if (camera.read(frame)) {
            return matToBufferedImage(frame);
        }
        return null;
    }

    public void saveImage(String filePath) {
        Mat frame = new Mat();
        if (camera.read(frame)) {
            Imgcodecs.imwrite(filePath, frame);
        }
    }

    public void releaseCamera() {
        camera.release();
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
        return image;
    }
}