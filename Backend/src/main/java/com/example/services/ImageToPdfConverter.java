package com.example.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service to convert image files (JPG, PNG, etc.) to PDF format
 * This allows image-based documents to be validated using the same PDF validation logic
 */
@Service
public class ImageToPdfConverter {

    private static final Logger logger = LoggerFactory.getLogger(ImageToPdfConverter.class);

    /**
     * Check if the file is an image file
     */
    public boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            // Check by extension
            String fileName = file.getOriginalFilename();
            if (fileName != null) {
                String lowerName = fileName.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || lowerName.endsWith(".gif") || 
                       lowerName.endsWith(".bmp") || lowerName.endsWith(".tiff");
            }
            return false;
        }
        
        return contentType.startsWith("image/");
    }

    /**
     * Convert an image file to PDF
     * @param imageFile The image file to convert
     * @return A MultipartFile representing the converted PDF, or null if conversion fails
     */
    public MultipartFile convertImageToPdf(MultipartFile imageFile) {
        if (!isImageFile(imageFile)) {
            logger.warn("File {} is not an image file", imageFile.getOriginalFilename());
            return null;
        }

        try (InputStream imageInputStream = imageFile.getInputStream()) {
            // Read the image
            BufferedImage bufferedImage = ImageIO.read(imageInputStream);
            if (bufferedImage == null) {
                logger.error("Could not read image from file: {}", imageFile.getOriginalFilename());
                return null;
            }

            // Validate image dimensions
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            
            if (width <= 0 || height <= 0) {
                logger.error("Invalid image dimensions: {}x{} for file: {}", 
                    width, height, imageFile.getOriginalFilename());
                return null;
            }

            logger.info("Converting image {}x{} pixels to PDF", width, height);

            // Create PDF document
            try (PDDocument document = new PDDocument()) {
                // Create a page with the same dimensions as the image
                // Use standard page size if dimensions are too large (PDFBox limit)
                float pdfWidth = Math.min(width, 14400); // Max width ~200 inches at 72 DPI
                float pdfHeight = Math.min(height, 14400); // Max height ~200 inches at 72 DPI
                
                PDPage page = new PDPage(new PDRectangle(pdfWidth, pdfHeight));
                document.addPage(page);

                // Convert BufferedImage to PDImageXObject using LosslessFactory
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

                // Draw the image on the page
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, pdfWidth, pdfHeight);
                }

                // Convert PDF to byte array
                ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
                document.save(pdfOutputStream);
                byte[] pdfBytes = pdfOutputStream.toByteArray();

                if (pdfBytes == null || pdfBytes.length == 0) {
                    logger.error("Generated PDF is empty for file: {}", imageFile.getOriginalFilename());
                    return null;
                }

                logger.info("Successfully created PDF: {} bytes", pdfBytes.length);

                // Create a new MultipartFile wrapper for the PDF
                String originalName = imageFile.getOriginalFilename();
                String pdfName;
                
                if (originalName != null && originalName.contains(".")) {
                    int lastDotIndex = originalName.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        pdfName = originalName.substring(0, lastDotIndex) + ".pdf";
                    } else {
                        pdfName = originalName + ".pdf";
                    }
                } else {
                    pdfName = "converted_" + System.currentTimeMillis() + ".pdf";
                }

                logger.info("Converted image {} to PDF: {}", originalName, pdfName);
                return new PdfMultipartFile(pdfBytes, pdfName);

            } catch (IOException e) {
                logger.error("Error creating PDF from image: {}", e.getMessage(), e);
                return null;
            } catch (Exception e) {
                logger.error("Unexpected error converting image to PDF: {}", e.getMessage(), e);
                return null;
            }

        } catch (IOException e) {
            logger.error("Error reading image file: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error processing image file: {}", e.getMessage(), e);
            return null;
        }
    }


    /**
     * Simple MultipartFile implementation for converted PDF
     */
    private static class PdfMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;

        public PdfMultipartFile(byte[] content, String name) {
            this.content = content;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return "application/pdf";
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
