package student_management_system.service;

import student_management_system.model.SchoolClass;
import student_management_system.model.User;
import student_management_system.repository.SchoolClassRepository;
import student_management_system.repository.UserRepository;
import student_management_system.web.dto.ClassDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Import PasswordEncoder if you decide to hash the join password
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the ClassService interface.
 * UPDATED: Corrected file path resolution in storeClassImage.
 */
@Service
public class ClassServiceImpl implements ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassServiceImpl.class);

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private UserRepository userRepository;

    // Optional: Inject PasswordEncoder if hashing join password
    // @Autowired
    // private PasswordEncoder passwordEncoder;

    @Value("${file.class-image-upload-dir}")
    private String classImageUploadDir;

    @Override
    @Transactional
    public SchoolClass createClass(ClassDto classDto, User teacher) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(classDto.getName());
        schoolClass.setDescription(classDto.getDescription());
        schoolClass.setTeacher(teacher);
        schoolClass.setClassCode(generateUniqueClassCode());

        if (StringUtils.hasText(classDto.getJoinPassword())) {
            schoolClass.setJoinPassword(classDto.getJoinPassword().trim());
            logger.info("Setting join password for class '{}'", classDto.getName());
        } else {
            schoolClass.setJoinPassword(null);
        }

        MultipartFile imageFile = classDto.getClassImageFile();
        if (imageFile != null && !imageFile.isEmpty()) {
            String storedFilename = storeClassImage(imageFile);
            schoolClass.setClassImagePath(storedFilename);
        }

        logger.info("Creating new class '{}' by teacher '{}'", classDto.getName(), teacher.getUsername());
        return schoolClassRepository.save(schoolClass);
    }

    @Override
    @Transactional
    public SchoolClass updateClass(Long classId, ClassDto classDto, User teacher) {
        SchoolClass existingClass = findClassById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with ID: " + classId));

        if (!existingClass.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("You are not authorized to update this class.");
        }

        existingClass.setName(classDto.getName());
        existingClass.setDescription(classDto.getDescription());

        if (classDto.getJoinPassword() != null) {
             if (StringUtils.hasText(classDto.getJoinPassword())) {
                 existingClass.setJoinPassword(classDto.getJoinPassword().trim());
                 logger.info("Updating join password for class ID {}", classId);
             } else {
                 existingClass.setJoinPassword(null);
                 logger.info("Removing join password for class ID {}", classId);
             }
        }

        MultipartFile newImageFile = classDto.getClassImageFile();
        if (newImageFile != null && !newImageFile.isEmpty()) {
            deleteClassImageFile(existingClass.getClassImagePath()); // Delete old one if exists
            String newStoredFilename = storeClassImage(newImageFile);
            existingClass.setClassImagePath(newStoredFilename);
        }

        logger.info("Updating class ID {} by teacher '{}'", classId, teacher.getUsername());
        return schoolClassRepository.save(existingClass);
    }


    @Override
    @Transactional
    public void deleteClass(Long classId, User teacher) {
        SchoolClass classToDelete = findClassById(classId)
                 .orElseThrow(() -> new EntityNotFoundException("Class not found with ID: " + classId));
        if (!classToDelete.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this class.");
        }
        deleteClassImageFile(classToDelete.getClassImagePath());
        logger.warn("Deleting class ID: {}. Associated assignments/submissions may cascade delete.", classId);
        schoolClassRepository.deleteById(classId);
    }

    private String storeClassImage(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = ""; int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) { fileExtension = originalFilename.substring(lastDot); }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // **FIX APPLIED HERE**
        Path uploadPath = Paths.get(classImageUploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(uniqueFilename).normalize();

        try {
            if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); logger.info("Created class image directory: {}", uploadPath); }

            // This check should now work correctly
            if (!filePath.startsWith(uploadPath)) {
                throw new RuntimeException("Cannot store file with relative path outside current directory " + originalFilename);
            }
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored class image file: {}", filePath);
            return uniqueFilename;
        } catch (IOException e) {
            logger.error("Could not store class image file {}: {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Could not store class image file " + originalFilename + ". Please try again!", e);
        }
    }

    private void deleteClassImageFile(String filename) {
        if (!StringUtils.hasText(filename)) { return; }
        try {
            // **Ensure consistent path resolution for deletion too**
            Path uploadPath = Paths.get(classImageUploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (Files.exists(filePath)) { Files.delete(filePath); logger.info("Deleted old class image file: {}", filePath); }
            else { logger.warn("Old class image file not found for deletion: {}", filePath); }
        } catch (IOException e) { logger.error("Error deleting old class image file '{}': {}", filename, e.getMessage(), e); }
        catch (Exception e) { logger.error("Unexpected error deleting old class image file '{}': {}", filename, e.getMessage(), e); }
    }

    @Override
    public List<SchoolClass> findClassesByTeacher(User teacher) { return schoolClassRepository.findByTeacher(teacher); }
    @Override
    public Optional<SchoolClass> findClassById(Long classId) { return schoolClassRepository.findById(classId); }
    @Override
    public Optional<SchoolClass> findClassByCode(String classCode) { return schoolClassRepository.findByClassCode(classCode); }

    @Override
    @Transactional
    public SchoolClass enrollStudent(User student, String classCode, String joinPassword) {
        SchoolClass schoolClass = schoolClassRepository.findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Class with code '" + classCode + "' not found."));

        if (schoolClass.getStudents().contains(student)) {
            throw new RuntimeException("Student already enrolled in class '" + schoolClass.getName() + "'.");
        }

        if (StringUtils.hasText(schoolClass.getJoinPassword())) {
             if (!StringUtils.hasText(joinPassword)) {
                 throw new RuntimeException("This class requires a password to join.");
             }
             if (!schoolClass.getJoinPassword().equals(joinPassword.trim())) {
                  throw new RuntimeException("Incorrect join password for class '" + schoolClass.getName() + "'.");
             }
        }

        schoolClass.addStudent(student);
        logger.info("Student {} enrolled in class '{}' (ID: {})", student.getUsername(), schoolClass.getName(), schoolClass.getId());
        return schoolClassRepository.save(schoolClass);
    }

     @Override
     public Set<SchoolClass> findClassesByStudent(User student) {
        User managedStudent = userRepository.findById(student.getId()).orElseThrow(() -> new EntityNotFoundException("Student not found"));
        return managedStudent.getEnrolledClasses();
     }

    private String generateUniqueClassCode() {
        String code;
        do { code = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6).toUpperCase(); }
        while (schoolClassRepository.findByClassCode(code).isPresent());
        return code;
    }
}
