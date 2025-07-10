package student_management_system.controller; // Ensure package matches

import student_management_system.service.UserService; // Import the INTERFACE
import student_management_system.web.dto.UserRegistrationDto; // Import DTO

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller to handle authentication and registration requests.
 */
@Controller
public class AuthController {

    // Inject the UserService INTERFACE, not the implementation
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @ModelAttribute("userDto") UserRegistrationDto userDto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Add validation logic here later
        if (result.hasErrors()) {
            model.addAttribute("userDto", userDto);
            return "register";
        }

        try {
            userService.createStudent(userDto); // Call method from the interface
            redirectAttributes.addFlashAttribute("registrationSuccess",
                    "Registration successful! Please log in.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            model.addAttribute("userDto", userDto);
            model.addAttribute("registrationError", e.getMessage());
            return "register";
        }
    }

    // --- NO OTHER CODE (like UserServiceImpl) SHOULD BE IN THIS FILE ---
}
