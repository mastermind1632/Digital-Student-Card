package tut.ac.za.dsc.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Checks an uploaded photo by its content (not its file name), crops it to the 3:4 card shape and
 * stores it as a 480 x 640 JPEG, so every card looks the same and the database stays small.
 */
public final class PhotoProcessor {

    public static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
    private static final int OUT_W = 480;
    private static final int OUT_H = 640;

    private PhotoProcessor() {
    }

    public static byte[] process(byte[] data) {
        if (data == null || data.length == 0) throw ValidationException.of("photo", "Choose a photo to upload.");
        if (data.length > MAX_UPLOAD_BYTES) throw ValidationException.of("photo", "The photo is larger than 5 MB. Choose a smaller photo.");
        if (!isJpeg(data) && !isPng(data)) throw ValidationException.of("photo", "Upload a JPG or PNG photo.");
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            src = null;
        }
        if (src == null) throw ValidationException.of("photo", "This photo could not be opened. Try another JPG or PNG file.");
        if (src.getWidth() < 200 || src.getHeight() < 200) {
            throw ValidationException.of("photo", "The photo is too small. Use one that is at least 200 x 200 pixels.");
        }
        int w = src.getWidth(), h = src.getHeight();
        int cropW = w, cropH = (int) Math.round(w * 4.0 / 3.0);
        if (cropH > h) {
            cropH = h;
            cropW = (int) Math.round(h * 3.0 / 4.0);
        }
        int x = (w - cropW) / 2;
        int y = Math.max(0, (int) ((h - cropH) * 0.35)); // keep a little more of the top, where the face usually is

        BufferedImage out = new BufferedImage(OUT_W, OUT_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, OUT_W, OUT_H);
        g.drawImage(src, 0, 0, OUT_W, OUT_H, x, y, x + cropW, y + cropH, null);
        g.dispose();
        return toJpeg(out, 0.88f);
    }

    static boolean isJpeg(byte[] d) {
        return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    static boolean isPng(byte[] d) {
        return d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
    }

    private static byte[] toJpeg(BufferedImage img, float quality) {
        try {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam p = writer.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(quality);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(img, null, null), p);
            } finally {
                writer.dispose();
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the photo", e);
        }
    }
}
