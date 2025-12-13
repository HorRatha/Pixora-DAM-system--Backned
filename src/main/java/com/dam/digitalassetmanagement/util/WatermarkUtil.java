package com.dam.digitalassetmanagement.util;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Component
public class WatermarkUtil {

    @Value("${app.watermark-enabled:true}")
    private boolean watermarkEnabled;

    @Value("${app.watermark-text:© DAM System}")
    private String watermarkText;

    @Value("${app.thumbnail-width:300}")
    private int thumbnailWidth;

    @Value("${app.thumbnail-height:300}")
    private int thumbnailHeight;

    public File createThumbnail(File originalFile, String outputPath) throws IOException {
        File thumbnailFile = new File(outputPath);

        Thumbnails.of(originalFile)
                .size(thumbnailWidth, thumbnailHeight)
                .toFile(thumbnailFile);

        return thumbnailFile;
    }

    public File addWatermark(File originalFile, String outputPath) throws IOException {
        if (!watermarkEnabled) {
            return originalFile;
        }

        BufferedImage originalImage = ImageIO.read(originalFile);
        Graphics2D g2d = (Graphics2D) originalImage.getGraphics();

        // Set watermark properties
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        g2d.setComposite(alphaChannel);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));

        // Calculate position (bottom-right corner)
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int x = originalImage.getWidth() - fontMetrics.stringWidth(watermarkText) - 10;
        int y = originalImage.getHeight() - 10;

        // Draw watermark
        g2d.drawString(watermarkText, x, y);
        g2d.dispose();

        // Save watermarked image
        File watermarkedFile = new File(outputPath);
        ImageIO.write(originalImage, "png", watermarkedFile);

        return watermarkedFile;
    }

    public File createWatermarkedThumbnail(File originalFile, String outputPath) throws IOException {
        // First create thumbnail
        BufferedImage thumbnail = Thumbnails.of(originalFile)
                .size(thumbnailWidth, thumbnailHeight)
                .asBufferedImage();

        if (watermarkEnabled) {
            // Add watermark to thumbnail
            Graphics2D g2d = (Graphics2D) thumbnail.getGraphics();
            AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
            g2d.setComposite(alphaChannel);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));

            FontMetrics fontMetrics = g2d.getFontMetrics();
            int x = thumbnail.getWidth() - fontMetrics.stringWidth(watermarkText) - 5;
            int y = thumbnail.getHeight() - 5;

            g2d.drawString(watermarkText, x, y);
            g2d.dispose();
        }

        // Save
        File thumbnailFile = new File(outputPath);
        ImageIO.write(thumbnail, "png", thumbnailFile);

        return thumbnailFile;
    }
}