package student_management_system.config; // Ensure this matches your package structure

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/", "/home", "/register", "/login", "/error", "/access-denied").permitAll()

                        // Authenticated user access (General)
                        .requestMatchers("/settings", "/settings/**").authenticated()
                        .requestMatchers("/download/profile/**").authenticated()
                        .requestMatchers("/friends", "/friends/**").authenticated()
                        .requestMatchers("/messages", "/messages/**").authenticated() // Added messages overview
                        .requestMatchers("/chat/private/**").authenticated() // Added private chat pages/history
                        .requestMatchers("/download/private-attachment/**").authenticated() // Added private attachments download

                        // Role-specific authorizations
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Teacher URLs
                        .requestMatchers("/teacher/**").hasRole("TEACHER")
                        // Explicitly allowing teacher quiz URLs (redundant if /teacher/** is allowed, but clearer)
                        .requestMatchers("/teacher/quizzes/**").hasRole("TEACHER")
                        .requestMatchers("/teacher/attempts/**").hasRole("TEACHER") // For grading attempts

                        // Student URLs
                        .requestMatchers("/student/**").hasRole("STUDENT")
                         // Explicitly allowing student quiz URLs (redundant if /student/** is allowed, but clearer)
                        .requestMatchers("/student/quizzes/**").hasRole("STUDENT")

                        // File Downloads (Consider role-specific access if needed, e.g., teacher can download all submissions)
                        // These might need more granular control depending on requirements
                        .requestMatchers("/download/assignment/**").authenticated()
                        .requestMatchers("/download/submission/**").authenticated()
                        .requestMatchers("/download/class-image/**").authenticated() // Allow authenticated users to see class images

                        // WebSocket Endpoint (Typically handled differently, but permit access for SockJS)
                        .requestMatchers("/ws/**").permitAll() // Allow SockJS connection

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.POST.name()))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                 .exceptionHandling(exceptions -> exceptions
                     .accessDeniedPage("/access-denied")
                 );

        // Important for H2 console if used, or specific frame options needed
        // http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        // Disable CSRF for WebSocket endpoint if STOMP clients have issues, but generally keep it enabled
        // http.csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**")); // Example if needed

        return http.build();
    }
}
