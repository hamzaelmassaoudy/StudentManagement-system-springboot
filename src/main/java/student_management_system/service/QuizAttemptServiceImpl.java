package student_management_system.service;

import student_management_system.model.*;
import student_management_system.model.QuizQuestion.QuestionType;
import student_management_system.repository.*;
import student_management_system.web.dto.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(QuizAttemptServiceImpl.class);

    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuizQuestionRepository quizQuestionRepository;
    @Autowired private QuizOptionRepository quizOptionRepository;
    @Autowired private QuizAnswerRepository quizAnswerRepository;

    @Override
    @Transactional
    public QuizAttempt startQuizAttempt(Quiz quiz, User student) {
        logger.info("Student {} starting attempt for quiz ID {}", student.getUsername(), quiz.getId());
        Optional<QuizAttempt> existingAttempt = quizAttemptRepository.findByStudentAndQuiz(student, quiz);

        // Allow resuming only if IN_PROGRESS
        if (existingAttempt.isPresent()) {
            if(existingAttempt.get().getStatus() == QuizAttempt.AttemptStatus.IN_PROGRESS) {
                 // Check if time limit already expired based on original start time
                 if (quiz.getTimeLimitMinutes() != null && existingAttempt.get().getStartTime() != null) {
                     LocalDateTime deadline = existingAttempt.get().getStartTime().plusMinutes(quiz.getTimeLimitMinutes());
                     if (LocalDateTime.now().isAfter(deadline)) {
                          logger.warn("Student {} cannot resume quiz ID {}: Time limit expired.", student.getUsername(), quiz.getId());
                          // Optionally force-submit the attempt here or just prevent resuming
                          // For simplicity, let's just prevent resuming an expired IN_PROGRESS attempt
                           throw new IllegalStateException("The time limit for this quiz attempt has expired.");
                     }
                 }
                 logger.info("Student {} resuming existing IN_PROGRESS attempt ID {} for quiz ID {}", student.getUsername(), existingAttempt.get().getId(), quiz.getId());
                 return existingAttempt.get();
            } else {
                 logger.warn("Student {} cannot start quiz ID {}: Attempt already submitted/graded.", student.getUsername(), quiz.getId());
                 throw new IllegalStateException("You have already completed this quiz.");
            }
        }

        // Check due date for new attempts
        if (quiz.getDueDate() != null && LocalDateTime.now().isAfter(quiz.getDueDate())) {
            logger.warn("Student {} cannot start quiz ID {}: Due date has passed.", student.getUsername(), quiz.getId());
            throw new IllegalStateException("The due date for this quiz has passed.");
        }

        // Create new attempt
        QuizAttempt attempt = new QuizAttempt(quiz, student);
        attempt.setStatus(QuizAttempt.AttemptStatus.IN_PROGRESS);
        attempt.setStartTime(LocalDateTime.now());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        logger.info("Created new quiz attempt ID {} for student {} on quiz ID {}", savedAttempt.getId(), student.getUsername(), quiz.getId());
        return savedAttempt;
    }

    @Override
    @Transactional
    public QuizAttempt submitQuizAttempt(Long attemptId, QuizSubmissionDto submissionDto, User student) {
        logger.info("Student {} submitting quiz attempt ID {}", student.getUsername(), attemptId);
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz attempt not found with ID: " + attemptId));

        if (!attempt.getStudent().getId().equals(student.getId())) {
            logger.warn("Authorization failed: Student {} attempted to submit attempt ID {} owned by {}",
                    student.getUsername(), attemptId, attempt.getStudent().getUsername());
            throw new AccessDeniedException("You can only submit your own quiz attempts.");
        }

        if (attempt.getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS) {
            logger.warn("Attempt submission failed: Quiz attempt ID {} is not in progress (Status: {}).", attemptId, attempt.getStatus());
            // Allow viewing results even if submitted again, redirect handled by controller
            return attempt; // Return existing attempt if already submitted/graded
        }

        Quiz quiz = attempt.getQuiz();
        LocalDateTime submissionTime = LocalDateTime.now();
        LocalDateTime calculatedEndTime = submissionTime; // Default end time is now

        // --- Time Limit Check ---
        boolean timeExpired = false;
        if (quiz.getTimeLimitMinutes() != null && attempt.getStartTime() != null) {
            LocalDateTime deadline = attempt.getStartTime().plusMinutes(quiz.getTimeLimitMinutes());
            if (submissionTime.isAfter(deadline)) {
                timeExpired = true;
                calculatedEndTime = deadline; // Cap end time at the deadline
                logger.warn("Time limit exceeded for attempt ID {}. Limit: {}, Submitted at: {}. Setting end time to deadline: {}",
                        attemptId, quiz.getTimeLimitMinutes(), submissionTime, calculatedEndTime);
            }
        }
        // --- End Time Limit Check ---

        // Clear existing answers before saving new ones (important for idempotency if submitted multiple times)
        List<QuizAnswer> existingAnswers = quizAnswerRepository.findByQuizAttempt(attempt);
        if (!existingAnswers.isEmpty()) {
            logger.debug("Deleting {} existing answers for attempt ID {}", existingAnswers.size(), attemptId);
            quizAnswerRepository.deleteAllInBatch(existingAnswers); // More efficient bulk delete
            attempt.getAnswers().clear(); // Clear the collection in the entity
            quizAttemptRepository.flush(); // Ensure deletes happen before inserts if needed
        }


        int maxScore = 0;
        boolean requiresManualGrading = false; // Assume false unless a short answer is found

        if (submissionDto.getAnswers() != null) { // Check if answers list is provided
            for (QuizAnswerDto answerDto : submissionDto.getAnswers()) {
                // Validate that questionId is present in the DTO
                 if (answerDto.getQuestionId() == null) {
                     logger.error("Submission for attempt ID {} contains an answer with a null questionId. Skipping this answer.", attemptId);
                     continue; // Skip this invalid answer DTO
                 }

                QuizQuestion question = quizQuestionRepository.findById(answerDto.getQuestionId())
                        .orElseThrow(() -> new EntityNotFoundException("Question not found with ID: " + answerDto.getQuestionId()));

                if (!question.getQuiz().getId().equals(quiz.getId())) {
                     logger.error("Data integrity error: Question ID {} does not belong to Quiz ID {}", question.getId(), quiz.getId());
                     throw new IllegalArgumentException("Invalid question ID submitted.");
                }

                QuizAnswer answer = new QuizAnswer(attempt, question);
                maxScore += question.getPoints();

                // Handle SHORT_ANSWER - requires manual grading
                if (question.getQuestionType() == QuestionType.SHORT_ANSWER) {
                    answer.setAnswerText(answerDto.getAnswerText());
                    answer.setPointsAwarded(null); // Requires manual grading
                    requiresManualGrading = true; // Mark that manual grading is needed
                } else {
                     // Handle other types if/when implemented (e.g., auto-grade multiple choice)
                     logger.warn("Unsupported question type {} encountered for question ID {} during submission of attempt ID {}. Storing answer text but requires manual grading.",
                                 question.getQuestionType(), question.getId(), attemptId);
                     answer.setAnswerText(answerDto.getAnswerText()); // Store text anyway? Or handle differently?
                     answer.setPointsAwarded(null); // Default to manual grading for unsupported types
                     requiresManualGrading = true;
                }
                attempt.addAnswer(answer); // Add the processed answer
            }
        } else {
             logger.warn("No answers provided in submission DTO for attempt ID {}", attemptId);
        }

        attempt.setEndTime(calculatedEndTime); // Use calculated time (now or deadline)
        attempt.setScore(null); // Always null on initial submit if short answers exist
        attempt.setMaxScore(maxScore);
        attempt.setStatus(QuizAttempt.AttemptStatus.SUBMITTED); // Always SUBMITTED initially

        QuizAttempt completedAttempt = quizAttemptRepository.save(attempt); // Save attempt and cascaded answers
        logger.info("Quiz attempt ID {} submitted successfully by student {}. Max Score: {}. Status: {}. Time Expired: {}",
                completedAttempt.getId(), student.getUsername(), completedAttempt.getMaxScore(), completedAttempt.getStatus(), timeExpired);

        // ** IMPORTANT: Do NOT throw exception here if time expired **
        // Let the controller redirect to results page regardless.
        // The results page can potentially show a "Submitted late" message if needed based on end time vs quiz due date.

        return completedAttempt;
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<QuizAttempt> findAttemptByIdForUser(Long attemptId, User accessingUser) {
        logger.debug("Finding attempt ID {} for user {}", attemptId, accessingUser.getUsername());
        Optional<QuizAttempt> attemptOpt = quizAttemptRepository.findById(attemptId);
        if (attemptOpt.isEmpty()) return Optional.empty();
        QuizAttempt attempt = attemptOpt.get();
        Quiz quiz = attempt.getQuiz();
        if (quiz == null || quiz.getSchoolClass() == null || quiz.getSchoolClass().getTeacher() == null) {
             logger.error("Data integrity issue: Attempt ID {} is missing quiz, class, or teacher information.", attemptId);
             // Decide how to handle - maybe return empty or throw specific exception
             return Optional.empty(); // Or throw new IllegalStateException("Attempt data incomplete");
        }
        boolean isStudentOwner = attempt.getStudent().getId().equals(accessingUser.getId());
        boolean isTeacherOwner = quiz.getSchoolClass().getTeacher().getId().equals(accessingUser.getId());
        if (isStudentOwner || isTeacherOwner) {
            logger.debug("User {} authorized to view attempt ID {}", accessingUser.getUsername(), attemptId);
            return attemptOpt;
        } else {
             logger.warn("Authorization failed: User {} attempted to access attempt ID {} without permission.", accessingUser.getUsername(), attemptId);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizAttempt> findAttemptByStudentAndQuiz(User student, Quiz quiz) {
        return quizAttemptRepository.findByStudentAndQuiz(student, quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> findAttemptsByQuizForTeacher(Long quizId, User teacher) {
        logger.debug("Finding attempts for quiz ID {} by teacher {}", quizId, teacher.getUsername());
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with ID: " + quizId));
        if (quiz.getSchoolClass() == null || quiz.getSchoolClass().getTeacher() == null || !quiz.getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
            logger.warn("Authorization failed: Teacher {} attempted to access attempts for quiz ID {} owned by another teacher.", teacher.getUsername(), quizId);
            throw new AccessDeniedException("You are not authorized to view attempts for this quiz.");
        }
        return quizAttemptRepository.findByQuiz(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> findAttemptsByStudent(User student) {
        logger.debug("Finding all attempts for student {}", student.getUsername());
        return quizAttemptRepository.findByStudent(student);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAttemptDto getAttemptResultDto(QuizAttempt attempt) {
        if (attempt == null) return null;
        QuizAttemptDto dto = new QuizAttemptDto();
        dto.setId(attempt.getId());
        dto.setQuizId(attempt.getQuiz().getId());
        dto.setQuizTitle(attempt.getQuiz().getTitle());
        dto.setStudentUsername(attempt.getStudent().getUsername());
        dto.setStudentFullName(attempt.getStudent().getFirstName() + " " + attempt.getStudent().getLastName());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());
        dto.setScore(attempt.getScore());
        dto.setMaxScore(attempt.getMaxScore());
        dto.setStatus(attempt.getStatus());
        List<QuizAnswerResultDto> answerResults = new ArrayList<>();
        // Eagerly fetch answers if needed, or rely on transaction
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizAttempt(attempt);
        logger.debug("Found {} answers for attempt ID {}", answers.size(), attempt.getId());

        // Create a map of questions for efficient lookup if needed, ensure questions are loaded
        Map<Long, QuizQuestion> questionMap = attempt.getQuiz().getQuestions().stream()
                                                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));

        for (QuizAnswer answer : answers) {
            QuizAnswerResultDto resultDto = new QuizAnswerResultDto();
            // Use the map for potentially better performance if questions were lazy loaded
            QuizQuestion question = questionMap.get(answer.getQuestion().getId());
            // QuizQuestion question = answer.getQuestion(); // Simpler if questions are eagerly loaded

            if (question == null) {
                logger.warn("Skipping answer ID {} because associated question (ID {}) could not be found.", answer.getId(), answer.getQuestion().getId());
                continue;
            }
            resultDto.setQuestionId(question.getId());
            resultDto.setQuestionText(question.getQuestionText());
            resultDto.setQuestionPoints(question.getPoints());
            resultDto.setPointsAwarded(answer.getPointsAwarded());

            if (question.getQuestionType() == QuestionType.SHORT_ANSWER) {
                resultDto.setStudentAnswerText(answer.getAnswerText());
                resultDto.setCorrectAnswerText("(Manual Grading Required)"); // Or teacher feedback if available?
                // Determine correctness based on awarded points vs possible points
                resultDto.setCorrect(answer.getPointsAwarded() != null && answer.getPointsAwarded().equals(Double.valueOf(question.getPoints())));
            } else {
                 // Placeholder for other types
                 resultDto.setStudentAnswerText("(Unsupported Question Type)");
                 resultDto.setCorrectAnswerText("N/A");
                 resultDto.setCorrect(false);
            }
            answerResults.add(resultDto);
        }
        dto.setAnswerResults(answerResults);
        logger.debug("Attempt ID {}: Prepared DTO with {} answer results.", attempt.getId(), answerResults.size());
        return dto;
    }

    @Override
    @Transactional
    public QuizAttempt gradeQuizAttempt(Long attemptId, GradeAttemptDto gradeAttemptDto, User teacher) {
        logger.info("Teacher {} attempting to grade quiz attempt ID {}", teacher.getUsername(), attemptId);

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz attempt not found with ID: " + attemptId));

        // Authorization Check
        if (attempt.getQuiz() == null || attempt.getQuiz().getSchoolClass() == null || attempt.getQuiz().getSchoolClass().getTeacher() == null ||
            !attempt.getQuiz().getSchoolClass().getTeacher().getId().equals(teacher.getId())) {
            logger.warn("Authorization failed: Teacher {} attempted to grade attempt ID {} owned by another teacher's class.",
                    teacher.getUsername(), attemptId);
            throw new AccessDeniedException("You are not authorized to grade this quiz attempt.");
        }

        if (attempt.getStatus() != QuizAttempt.AttemptStatus.SUBMITTED) {
            logger.warn("Grading failed: Quiz attempt ID {} is not in SUBMITTED status (Status: {}).", attemptId, attempt.getStatus());
            throw new IllegalStateException("This quiz attempt is not awaiting grading or has already been graded.");
        }

        double totalScore = 0.0;
        // Fetch answers associated with the attempt
        Map<Long, QuizAnswer> answerMap = quizAnswerRepository.findByQuizAttempt(attempt).stream()
                .collect(Collectors.toMap(ans -> ans.getQuestion().getId(), Function.identity()));

        if (gradeAttemptDto.getAnswerGrades() == null) {
             throw new IllegalArgumentException("Grading data (answer grades list) is missing.");
        }

        for (GradeAttemptDto.AnswerGradeDto answerGrade : gradeAttemptDto.getAnswerGrades()) {
            Long questionId = answerGrade.getQuestionId();
            Double pointsAwarded = answerGrade.getPointsAwarded();

            if (questionId == null) {
                 logger.warn("Skipping grade entry with null questionId for attempt ID {}", attemptId);
                 continue;
            }

            QuizAnswer answer = answerMap.get(questionId);
            if (answer == null) {
                logger.warn("Grading attempt ID {}: No answer found in the database for submitted grade's question ID {}. Skipping.", attemptId, questionId);
                continue; // Skip if no corresponding answer exists for the graded question
            }

            QuizQuestion question = answer.getQuestion();
            if (question == null) {
                 logger.error("Data integrity error: Answer ID {} has no associated question for attempt ID {}. Skipping.", answer.getId(), attemptId);
                 continue;
            }

            if (pointsAwarded == null) {
                // Allow null points (meaning 0), but log it. Or throw error if points must be explicit?
                logger.warn("Points awarded is null for question ID {} in attempt ID {}. Assuming 0 points.", questionId, attemptId);
                pointsAwarded = 0.0;
                // throw new IllegalArgumentException("Points awarded cannot be null for question ID: " + questionId);
            }
            if (pointsAwarded < 0 || pointsAwarded > question.getPoints()) {
                throw new IllegalArgumentException("Points awarded (" + pointsAwarded + ") for question ID " + questionId
                        + " must be between 0 and " + question.getPoints());
            }

            answer.setPointsAwarded(pointsAwarded);
            totalScore += pointsAwarded;
            logger.trace("Graded answer for question ID {} in attempt ID {} with {} points.", questionId, attemptId, pointsAwarded);
            // Note: We are saving the attempt at the end, which cascades saves to answers
        }

        attempt.setScore(totalScore);
        attempt.setStatus(QuizAttempt.AttemptStatus.GRADED); // Set status to GRADED

        QuizAttempt gradedAttempt = quizAttemptRepository.save(attempt);
        logger.info("Quiz attempt ID {} graded successfully by teacher {}. Final Score: {}/{}. Status set to GRADED.",
                gradedAttempt.getId(), teacher.getUsername(), gradedAttempt.getScore(), gradedAttempt.getMaxScore());

        return gradedAttempt;
    }

    // --- Implementation for findAttemptsByTeacherAndStatus ---
    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> findAttemptsByTeacherAndStatus(User teacher, QuizAttempt.AttemptStatus status, int limit) {
        logger.debug("Finding attempts with status {} for teacher {} (limit {})", status, teacher.getUsername(), limit);

        // 1. Find all quizzes taught by the teacher
        List<Quiz> teacherQuizzes = quizRepository.findBySchoolClass_Teacher(teacher);
        if (teacherQuizzes.isEmpty()) {
            logger.debug("Teacher {} teaches no quizzes, returning empty list.", teacher.getUsername());
            return Collections.emptyList();
        }
        logger.trace("Teacher {} teaches {} quizzes.", teacher.getUsername(), teacherQuizzes.size());

        // 2. Find attempts for those quizzes with the specified status, limited and sorted
        // Sort by submission time (endTime) ascending to show oldest first
        Pageable pageable = PageRequest.of(0, limit, Sort.by("endTime").ascending());
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizInAndStatus(teacherQuizzes, status, pageable);
        logger.debug("Found {} attempts with status {} for teacher {} quizzes (limit {})", attempts.size(), status, teacher.getUsername(), limit);

        return attempts;
    }

} // End of class QuizAttemptServiceImpl
