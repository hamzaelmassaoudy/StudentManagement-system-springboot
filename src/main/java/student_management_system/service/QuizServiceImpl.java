// src/main/java/student_management_system/service/QuizServiceImpl.java
package student_management_system.service;

import student_management_system.model.*;
import student_management_system.model.QuizQuestion.QuestionType; // Import enum
import student_management_system.repository.*;
import student_management_system.web.dto.QuizDto;
import student_management_system.web.dto.QuizQuestionDto;
// Removed import for QuizOptionDto as it's no longer used here

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizServiceImpl.class);

    @Autowired private QuizRepository quizRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuizQuestionRepository quizQuestionRepository;
    @Autowired private QuizOptionRepository quizOptionRepository; // Keep for potential cleanup if needed
    @Autowired private QuizAttemptRepository quizAttemptRepository;

    @Override
    @Transactional
    public Quiz createQuiz(QuizDto quizDto, Long classId, User teacher) {
        logger.info("Attempting to create quiz '{}' for class ID {} by teacher {}", quizDto.getTitle(), classId, teacher.getUsername());

        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with ID: " + classId));

        if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
            logger.warn("Authorization failed: Teacher {} attempted to create quiz for class ID {} owned by {}",
                    teacher.getUsername(), classId, schoolClass.getTeacher().getUsername());
            throw new AccessDeniedException("You are not authorized to create quizzes for this class.");
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(quizDto.getTitle());
        quiz.setDescription(quizDto.getDescription());
        quiz.setDueDate(quizDto.getDueDate());
        quiz.setTimeLimitMinutes(quizDto.getTimeLimitMinutes());
        quiz.setSchoolClass(schoolClass);

        int questionOrder = 0;
        if (quizDto.getQuestions() != null) {
            for (QuizQuestionDto questionDto : quizDto.getQuestions()) {
                if (questionDto.getQuestionText() == null || questionDto.getQuestionText().trim().isEmpty()) {
                    logger.warn("Skipping question with empty text during quiz creation for class ID {}", classId);
                    continue;
                }

                QuizQuestion question = new QuizQuestion();
                question.setQuestionText(questionDto.getQuestionText());
                // --- MODIFIED: Always set type to SHORT_ANSWER ---
                question.setQuestionType(QuestionType.SHORT_ANSWER);
                // --- END MODIFIED ---
                question.setPoints(questionDto.getPoints());
                question.setQuestionOrder(questionOrder++);

                // --- REMOVED: Option mapping logic ---
                // if (questionDto.getQuestionType() == QuestionType.MULTIPLE_CHOICE) { ... }
                // --- END REMOVED ---

                quiz.addQuestion(question);
            }
        }

        if (quiz.getQuestions().isEmpty()) {
            logger.error("Validation failed for quiz '{}': Quiz must have at least one valid question.", quiz.getTitle());
            throw new IllegalArgumentException("Quiz must contain at least one question.");
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        logger.info("Successfully created quiz ID {} with {} questions for class ID {}", savedQuiz.getId(), savedQuiz.getQuestions().size(), classId);
        return savedQuiz;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quiz> findQuizByIdForUser(Long quizId, User accessingUser) {
        // Authorization logic remains the same
        logger.debug("Attempting to find quiz ID {} for user {}", quizId, accessingUser.getUsername());
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);

        if (quizOpt.isEmpty()) {
            logger.warn("Quiz ID {} not found.", quizId);
            return Optional.empty();
        }

        Quiz quiz = quizOpt.get();
        SchoolClass schoolClass = quiz.getSchoolClass();
        if (schoolClass == null || schoolClass.getTeacher() == null) {
             logger.error("Data integrity issue: Quiz ID {} has no associated class or teacher.", quizId);
             return Optional.empty();
        }

        boolean isTeacher = schoolClass.getTeacher().getId().equals(accessingUser.getId());
        User userWithClasses = userRepository.findById(accessingUser.getId()).orElse(accessingUser);
        boolean isStudentEnrolled = userWithClasses.getEnrolledClasses().stream()
                                        .anyMatch(enrolledClass -> enrolledClass.getId().equals(schoolClass.getId()));

        if (isTeacher || isStudentEnrolled) {
            logger.debug("Authorization successful for user {} to access quiz ID {}", accessingUser.getUsername(), quizId);
            return quizOpt;
        } else {
            logger.warn("Authorization failed: User {} attempted to access quiz ID {} for class ID {} without permission.",
                    accessingUser.getUsername(), quizId, schoolClass.getId());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quiz> findQuizzesByClassId(Long classId) {
        logger.debug("Finding quizzes for class ID {}", classId);
        return quizRepository.findBySchoolClassId(classId);
    }

    @Override
    @Transactional
    public Quiz updateQuiz(Long quizId, QuizDto quizDto, User teacher) {
        logger.info("Attempting to update quiz ID {} by teacher {}", quizId, teacher.getUsername());

        Quiz existingQuiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with ID: " + quizId));

        if (existingQuiz.getSchoolClass() == null || existingQuiz.getSchoolClass().getTeacher() == null ||
            !existingQuiz.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
             logger.warn("Authorization failed: Teacher {} attempted to update quiz ID {} owned by another teacher or class is invalid.",
                    teacher.getUsername(), quizId);
            throw new AccessDeniedException("You are not authorized to update this quiz.");
        }

        existingQuiz.setTitle(quizDto.getTitle());
        existingQuiz.setDescription(quizDto.getDescription());
        existingQuiz.setDueDate(quizDto.getDueDate());
        existingQuiz.setTimeLimitMinutes(quizDto.getTimeLimitMinutes());

        // --- Handle Question Updates (Simplified: remove old, add new) ---
        existingQuiz.getQuestions().clear();
        // quizRepository.flush(); // Optional flush

        int questionOrder = 0;
        if (quizDto.getQuestions() != null) {
             for (QuizQuestionDto questionDto : quizDto.getQuestions()) {
                 if (questionDto.getQuestionText() == null || questionDto.getQuestionText().trim().isEmpty()) {
                    logger.warn("Skipping question with empty text during quiz update for quiz ID {}", quizId);
                    continue;
                }

                QuizQuestion question = new QuizQuestion();
                question.setQuestionText(questionDto.getQuestionText());
                // --- MODIFIED: Always set type to SHORT_ANSWER ---
                question.setQuestionType(QuestionType.SHORT_ANSWER);
                // --- END MODIFIED ---
                question.setPoints(questionDto.getPoints());
                question.setQuestionOrder(questionOrder++);

                // --- REMOVED: Option mapping logic ---
                // if (questionDto.getQuestionType() == QuestionType.MULTIPLE_CHOICE) { ... }
                // --- END REMOVED ---

                 existingQuiz.addQuestion(question);
            }
        }
         if (existingQuiz.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Quiz must contain at least one question.");
        }

        Quiz updatedQuiz = quizRepository.save(existingQuiz);
        logger.info("Successfully updated quiz ID {}", quizId);
        return updatedQuiz;
    }

    @Override
    @Transactional
    public void deleteQuiz(Long quizId, User teacher) {
        // Deletion logic remains the same, cascade handles questions/options/attempts
        logger.warn("Attempting to delete quiz ID {} by teacher {}", quizId, teacher.getUsername());
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with ID: " + quizId));

        if (quiz.getSchoolClass() == null || quiz.getSchoolClass().getTeacher() == null ||
            !quiz.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
             logger.warn("Authorization failed: Teacher {} attempted to delete quiz ID {} owned by another teacher or class is invalid.",
                    teacher.getUsername(), quizId);
            throw new AccessDeniedException("You are not authorized to delete this quiz.");
        }

        quizRepository.delete(quiz);
        logger.warn("Successfully deleted quiz ID {} and potentially related data due to cascade.", quizId);
    }

     @Override
     @Transactional(readOnly = true)
     public Quiz getQuizForTaking(Long quizId, User student) {
         // Logic remains largely the same
         logger.debug("Fetching quiz ID {} for student {} to take", quizId, student.getUsername());
         Quiz quiz = quizRepository.findById(quizId)
                 .orElseThrow(() -> new EntityNotFoundException("Quiz not found with ID: " + quizId));

         SchoolClass schoolClass = quiz.getSchoolClass();
         if (schoolClass == null) {
             throw new EntityNotFoundException("Quiz " + quizId + " is not associated with any class.");
         }
         User studentWithClasses = userRepository.findById(student.getId()).orElseThrow();
         if (!studentWithClasses.getEnrolledClasses().contains(schoolClass)) {
             logger.warn("Authorization failed: Student {} is not enrolled in class ID {} required for quiz ID {}",
                     student.getUsername(), schoolClass.getId(), quizId);
             throw new AccessDeniedException("You are not enrolled in the class for this quiz.");
         }

         if (quiz.getDueDate() != null && LocalDateTime.now().isAfter(quiz.getDueDate())) {
             logger.warn("Attempt failed: Quiz ID {} is past its due date ({}) for student {}",
                     quizId, quiz.getDueDate(), student.getUsername());
             throw new IllegalStateException("The due date for this quiz has passed.");
         }

         Optional<QuizAttempt> existingAttempt = quizAttemptRepository.findByStudentAndQuiz(student, quiz);
         if (existingAttempt.isPresent() && existingAttempt.get().getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS) {
             logger.warn("Attempt failed: Student {} has already submitted an attempt for quiz ID {}",
                     student.getUsername(), quizId);
             throw new IllegalStateException("You have already completed this quiz.");
         }

         logger.debug("Student {} is authorized and eligible to take quiz ID {}", student.getUsername(), quizId);
         return quiz;
     }
}
