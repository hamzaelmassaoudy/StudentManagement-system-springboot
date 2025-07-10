package student_management_system.controller;

import student_management_system.model.User;
import student_management_system.service.UserService;
import student_management_system.web.dto.UserRegistrationDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // For potential validation later
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable; // Import PathVariable
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // For flash messages

import jakarta.persistence.EntityNotFoundException; // Import exception

import java.util.List;

/**
 * Controller handling requests for the Administrator role.
 * Base path is /admin as configured in SecurityConfig.
 */
@Controller
@RequestMapping("/admin") // All mappings in this controller will start with /admin
public class AdminController {

    @Autowired
    private UserService userService;

    /**
     * Handles GET requests to /admin/teachers.
     * Fetches the list of all teachers and displays them.
     */
    @GetMapping("/teachers")
    public String listTeachers(Model model) {
        List<User> teachers = userService.findAllTeachers();
        model.addAttribute("teachers", teachers);
        return "admin/teachers"; // templates/admin/teachers.html
    }

    /**
     * Handles GET requests to /admin/teachers/new.
     * Displays the form for adding a new teacher.
     */
    @GetMapping("/teachers/new")
    public String showAddTeacherForm(Model model) {
        model.addAttribute("teacherDto", new UserRegistrationDto());
        return "admin/teacher-form"; // templates/admin/teacher-form.html
    }

    /**
     * Handles POST requests to /admin/teachers/create.
     * Processes the submitted form data to create a new teacher.
     */
    @PostMapping("/teachers/create")
    public String createTeacher(
            @ModelAttribute("teacherDto") UserRegistrationDto teacherDto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Add validation logic here later
        if (result.hasErrors()) {
            model.addAttribute("teacherDto", teacherDto);
            return "admin/teacher-form";
        }

        try {
            userService.createTeacher(teacherDto);
            redirectAttributes.addFlashAttribute("successMessage", "Teacher created successfully!");
            return "redirect:/admin/teachers";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating teacher: " + e.getMessage());
             return "redirect:/admin/teachers/new";
        }
    }

    // --- New Methods for Edit/Delete ---

    /**
     * Handles GET requests to /admin/teachers/edit/{id}.
     * Displays the form for editing an existing teacher.
     * @param id The ID of the teacher to edit.
     * @param model The Spring Model object.
     * @param redirectAttributes For error messages if teacher not found.
     * @return The view name "admin/teacher-edit-form".
     */
    @GetMapping("/teachers/edit/{id}")
    public String showEditTeacherForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findUserById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher not found with ID: " + id));

            // Basic check if the found user is actually a teacher (optional but good practice)
            boolean isTeacher = teacher.getRoles().stream().anyMatch(role -> "ROLE_TEACHER".equals(role.getName()));
            if (!isTeacher) {
                 redirectAttributes.addFlashAttribute("errorMessage", "User with ID " + id + " is not a teacher.");
                 return "redirect:/admin/teachers";
            }

            // Create DTO from entity to pre-populate the form
            // Note: We don't send the password to the form
            UserRegistrationDto teacherDto = new UserRegistrationDto(
                teacher.getFirstName(),
                teacher.getLastName(),
                null, // Don't need studentId for teachers
                teacher.getUsername(),
                null // IMPORTANT: Never send existing password hash to form
            );

            model.addAttribute("teacherDto", teacherDto);
            model.addAttribute("teacherId", id); // Pass the ID separately for the form action

            // Create a new template for editing or reuse teacher-form with modifications
            return "admin/teacher-edit-form"; // templates/admin/teacher-edit-form.html

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/teachers";
        }
    }

    /**
     * Handles POST requests to /admin/teachers/update/{id}.
     * Processes the submitted form data to update an existing teacher.
     * @param id The ID of the teacher to update.
     * @param teacherDto The DTO bound to the form data.
     * @param result BindingResult for validation.
     * @param redirectAttributes Used to pass messages after redirect.
     * @param model The Spring Model object.
     * @return Redirects to the teacher list on success, or back to the edit form on error.
     */
    @PostMapping("/teachers/update/{id}")
    public String updateTeacher(
            @PathVariable Long id,
            @ModelAttribute("teacherDto") UserRegistrationDto teacherDto, // Get DTO from form
            BindingResult result, // For validation results
            RedirectAttributes redirectAttributes,
            Model model) {

        // Add validation logic here later
        if (result.hasErrors()) {
            model.addAttribute("teacherDto", teacherDto);
            model.addAttribute("teacherId", id); // Need ID again for form action
            return "admin/teacher-edit-form"; // Return to the edit form view
        }

        try {
            userService.updateTeacher(id, teacherDto);
            redirectAttributes.addFlashAttribute("successMessage", "Teacher updated successfully!");
            return "redirect:/admin/teachers"; // Redirect to the list view on success
        } catch (EntityNotFoundException e) {
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/admin/teachers"; // Redirect to list if teacher not found during update
        } catch (RuntimeException e) {
            // Handle other errors like duplicate email if username changes were allowed
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating teacher: " + e.getMessage());
             return "redirect:/admin/teachers/edit/" + id; // Redirect back to the edit form
        }
    }

     /**
     * Handles POST requests to /admin/teachers/delete/{id}.
     * Deletes the specified teacher account.
     * @param id The ID of the teacher to delete.
     * @param redirectAttributes Used for success/error messages.
     * @return Redirects back to the teacher list.
     */
    @PostMapping("/teachers/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteTeacher(id);
            redirectAttributes.addFlashAttribute("successMessage", "Teacher deleted successfully!");
        } catch (EntityNotFoundException e) {
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Catch other potential errors (e.g., database constraints if cascade isn't set up correctly)
             redirectAttributes.addFlashAttribute("errorMessage", "Error deleting teacher: " + e.getMessage());
             System.err.println("Error deleting teacher ID " + id + ": " + e.getMessage()); // Log error
        }
        return "redirect:/admin/teachers"; // Redirect back to the list view
    }

}
