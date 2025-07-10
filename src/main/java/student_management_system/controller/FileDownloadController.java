package student_management_system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Controller dedicated to handling file download requests for
 * assignment attachments, student submissions, profile pictures,
 * private chat attachments, and class images.
 */
@Controller
public class FileDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    @Value("${file.assignment-upload-dir}")
    private String assignmentUploadDir;

    @Value("${file.submission-upload-dir}")
    private String submissionUploadDir;

    @Value("${file.profile-picture-upload-dir}")
    private String profileUploadDir;

    @Value("${file.private-attachment-upload-dir}")
    private String privateAttachmentUploadDir;

    // *** ADDED: Inject class image upload directory ***
    @Value("${file.class-image-upload-dir}")
    private String classImageUploadDir;
    // *** END ADDED ***


    @GetMapping("/download/assignment/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadAssignmentFile(@PathVariable String filename) {
        return downloadFile(assignmentUploadDir, filename, true); // Force attachment
    }

    @GetMapping("/download/submission/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadSubmissionFile(@PathVariable String filename) {
        return downloadFile(submissionUploadDir, filename, true); // Force attachment
    }

    @GetMapping("/download/profile/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadProfilePicture(@PathVariable String filename) {
        // For profile pics, try inline display first
        return downloadFile(profileUploadDir, filename, false);
    }

    @GetMapping("/download/private-attachment/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadPrivateAttachment(@PathVariable String filename) {
        // Force download for chat attachments
        return downloadFile(privateAttachmentUploadDir, filename, true);
    }

    // *** ADDED: Endpoint for class images ***
    @GetMapping("/download/class-image/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadClassImage(@PathVariable String filename) {
        // Allow inline display for class images
        return downloadFile(classImageUploadDir, filename, false);
    }
    // *** END ADDED ***


    /**
     * Helper method for downloading a file.
     * @param uploadDir The base directory.
     * @param filename The filename.
     * @param forceAttachment If true, forces download; otherwise, tries inline display.
     * @return ResponseEntity
     */
    private ResponseEntity<Resource> downloadFile(String uploadDir, String filename, boolean forceAttachment) {
        try {
            Path directoryPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = directoryPath.resolve(filename).normalize();

            logger.debug("Attempting to download file from path: {}", filePath);

            if (!filePath.startsWith(directoryPath)) {
                 logger.warn("Directory traversal attempt detected for filename: {}", filename);
                 return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                logger.debug("File found and readable: {}", filePath);
                String contentType = null;
                try {
                     contentType = Files.probeContentType(filePath);
                     logger.debug("Determined content type: {}", contentType);
                } catch (IOException e) {
                    logger.warn("Could not determine content type for file: {}", filePath, e);
                }
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // Fallback
                    logger.debug("Content type is null, falling back to: {}", contentType);
                }

                HttpHeaders headers = new HttpHeaders();
                String dispositionType = forceAttachment || contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                        ? "attachment" : "inline";
                headers.add(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + resource.getFilename() + "\"");
                logger.debug("Setting Content-Disposition to: {}", headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .headers(headers)
                        .body(resource);

            } else {
                logger.error("File not found or not readable: {}", filePath.toString());
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException for file: {}. Error: {}", filename, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
             logger.error("Error downloading file: {}. Error: {}", filename, e.getMessage(), e);
             return ResponseEntity.internalServerError().build();
        }
    }
}
