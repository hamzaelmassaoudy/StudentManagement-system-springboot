package student_management_system.controller;

import student_management_system.model.User;
import student_management_system.service.UserService;
import student_management_system.web.dto.ChangePasswordDto;
import student_management_system.web.dto.UserSettingsDto;

import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult; // Import if using validation
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Controller for handling user settings page requests.
 * UPDATED: Corrected file path resolution in updateProfilePicture.
 */
@Controller
@RequestMapping("/settings") // Base path for all settings-related requests
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class); // Add Logger

    @Autowired
    private UserService userService;

    @Value("${file.profile-picture-upload-dir}")
    private String profileUploadDir;

    @GetMapping
    public String showSettingsPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String username = userDetails.getUsername();
        User currentUser = userService.findUserByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found in database: " + username));

        UserSettingsDto settingsDto = new UserSettingsDto(
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getUsername()
        );
        ChangePasswordDto passwordDto = new ChangePasswordDto();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userSettingsDto", settingsDto);
        model.addAttribute("changePasswordDto", passwordDto);

        return "settings";
    }

    @PostMapping("/update/details")
    public String updateProfileDetails(@ModelAttribute("userSettingsDto") UserSettingsDto settingsDto,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        try {
            userService.updateUserSettings(username, settingsDto);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating details: " + e.getMessage());
            logger.error("Error updating profile details for {}: {}", username, e.getMessage(), e); // Log error
        }
        return "redirect:/settings";
    }

    @PostMapping("/update/password")
    public String changePassword(@ModelAttribute("changePasswordDto") ChangePasswordDto passwordDto,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmNewPassword())) {
             redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
             return "redirect:/settings";
        }
        try {
            userService.changeUserPassword(username, passwordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
        } catch (AccessDeniedException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password change failed: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while changing password.");
            logger.error("Error changing password for {}: {}", username, e.getMessage(), e); // Log error
        }
        return "redirect:/settings";
    }

    @PostMapping("/update/picture")
    public String updateProfilePicture(@RequestParam("profilePictureFile") MultipartFile file,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        String username = userDetails.getUsername();

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
            return "redirect:/settings";
        }
        long maxSize = 10 * 1024 * 1024; // Using 10MB from application.properties
        if (file.getSize() > maxSize) {
             redirectAttributes.addFlashAttribute("errorMessage", "File size exceeds the limit of 10MB.");
             return "redirect:/settings";
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg") && !contentType.equals("image/gif"))) {
             redirectAttributes.addFlashAttribute("errorMessage", "Invalid file type. Only PNG, JPG, GIF allowed.");
             return "redirect:/settings";
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            fileExtension = originalFilename.substring(lastDot);
        }
        String uniqueFilename = username + "_" + UUID.randomUUID().toString() + fileExtension;

        // **FIX APPLIED HERE**
        Path uploadPath = Paths.get(profileUploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(uniqueFilename).normalize();

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created profile picture directory: {}", uploadPath);
            }
            // This check should now work correctly
            if (!filePath.startsWith(uploadPath)) {
                 throw new RuntimeException("Cannot store file with relative path outside current directory " + originalFilename);
            }
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved profile picture: {}", filePath.toString());
            userService.updateProfilePicturePath(username, uniqueFilename);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully!");
        } catch (IOException e) {
            logger.error("Could not save profile picture file {}: {}", originalFilename, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Could not upload profile picture. Please try again.");
        } catch (Exception e) {
            logger.error("Error updating profile picture for {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred.");
        }
        return "redirect:/settings";
    }
}
