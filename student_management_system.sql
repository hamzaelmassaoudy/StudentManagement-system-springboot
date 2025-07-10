-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 06, 2025 at 11:10 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `student_management_system`
--

-- --------------------------------------------------------

--
-- Table structure for table `answers`
--

CREATE TABLE `answers` (
  `id` bigint(20) NOT NULL,
  `answer_text` text NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `question_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `assignments`
--

CREATE TABLE `assignments` (
  `class_id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `due_date` datetime(6) NOT NULL,
  `id` bigint(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `attachment_original_filename` varchar(255) DEFAULT NULL,
  `attachment_path` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `assignments`
--

INSERT INTO `assignments` (`class_id`, `created_at`, `due_date`, `id`, `title`, `attachment_original_filename`, `attachment_path`, `description`) VALUES
(6, '2025-05-06 09:55:48.000000', '2025-05-06 09:57:00.000000', 12, 'Assign1', '9fd5a6b8-f346-439c-b813-3e402e5aab60_pexels-duy-s-house-of-photo-2150454349-31890680.jpg', 'f3749eb5-d025-4278-97c6-61e673b92d9e_9fd5a6b8-f346-439c-b813-3e402e5aab60_pexels-duy-s-house-of-photo-2150454349-31890680.jpg', 'Do it on time'),
(6, '2025-05-06 10:52:30.000000', '2025-05-05 22:56:00.000000', 13, 'Assign 2', NULL, NULL, 'do it on time\r\n'),
(6, '2025-05-30 08:34:04.000000', '4555-12-05 04:22:00.000000', 14, 'rrrrrrrrrrr', NULL, NULL, ''),
(6, '2025-05-30 08:49:01.000000', '2025-05-08 08:51:00.000000', 15, 'assignment', NULL, NULL, '11'),
(6, '2025-05-30 08:51:23.000000', '2025-05-31 12:55:00.000000', 16, 'ddddddddddddd', NULL, NULL, 'dddddddddd');

-- --------------------------------------------------------

--
-- Table structure for table `chat_messages`
--

CREATE TABLE `chat_messages` (
  `class_id` bigint(20) NOT NULL,
  `id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `content` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chat_messages`
--

INSERT INTO `chat_messages` (`class_id`, `id`, `sender_id`, `timestamp`, `content`) VALUES
(6, 1, 6, '2025-05-06 09:52:36.000000', 'Hello laoshi'),
(6, 2, 3, '2025-05-06 09:52:44.000000', 'hi'),
(6, 3, 6, '2025-05-06 09:53:10.000000', 'do we have an assignment today?'),
(6, 4, 3, '2025-05-06 09:53:46.000000', 'yes, I will lunch it soon. be aware to sumbit it on time!'),
(6, 5, 6, '2025-05-06 09:54:03.000000', 'sure laoshi, waiting for it');

-- --------------------------------------------------------

--
-- Table structure for table `classes`
--

CREATE TABLE `classes` (
  `id` bigint(20) NOT NULL,
  `teacher_id` bigint(20) NOT NULL,
  `class_code` varchar(20) NOT NULL,
  `join_password` varchar(60) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `class_image_path` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `classes`
--

INSERT INTO `classes` (`id`, `teacher_id`, `class_code`, `join_password`, `name`, `class_image_path`, `description`) VALUES
(6, 3, '0D0BC9', NULL, 'CST 2021', '4564551b-b2d5-493a-9088-c84f1ab81c30.jpg', '');

-- --------------------------------------------------------

--
-- Table structure for table `classes_students`
--

CREATE TABLE `classes_students` (
  `class_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `classes_students`
--

INSERT INTO `classes_students` (`class_id`, `student_id`) VALUES
(6, 6);

-- --------------------------------------------------------

--
-- Table structure for table `friend_requests`
--

CREATE TABLE `friend_requests` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint(20) NOT NULL,
  `receiver_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','PENDING','REJECTED') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `friend_requests`
--

INSERT INTO `friend_requests` (`created_at`, `id`, `receiver_id`, `sender_id`, `updated_at`, `status`) VALUES
('2025-05-06 09:08:19.000000', 1, 3, 6, '2025-05-06 09:09:15.000000', 'ACCEPTED'),
('2025-05-06 09:43:37.000000', 2, 6, 3, '2025-05-06 10:49:55.000000', 'ACCEPTED');

-- --------------------------------------------------------

--
-- Table structure for table `makeup_requests`
--

CREATE TABLE `makeup_requests` (
  `id` bigint(20) NOT NULL,
  `original_submission_id` bigint(20) NOT NULL,
  `requested_at` datetime(6) NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by_teacher_id` bigint(20) DEFAULT NULL,
  `student_id` bigint(20) NOT NULL,
  `reason` text NOT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `teacher_comment` text DEFAULT NULL,
  `submission_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `makeup_requests`
--

INSERT INTO `makeup_requests` (`id`, `original_submission_id`, `requested_at`, `reviewed_at`, `reviewed_by_teacher_id`, `student_id`, `reason`, `status`, `teacher_comment`, `submission_id`) VALUES
(15, 18, '2025-05-06 10:01:12.000000', '2025-05-06 10:01:27.000000', 3, 6, 'i want make up please', 'APPROVED', NULL, 18),
(16, 19, '2025-05-06 10:53:50.000000', '2025-05-30 06:30:19.000000', 3, 6, 'aaaa', 'APPROVED', NULL, 19),
(17, 20, '2025-05-30 08:34:54.000000', '2025-05-30 08:35:07.000000', 3, 6, 'gfghgjh', 'APPROVED', NULL, 20),
(18, 21, '2025-05-30 08:52:33.000000', '2025-05-30 08:52:46.000000', 3, 6, 'ccc', 'APPROVED', NULL, 21),
(19, 22, '2025-05-30 09:00:59.000000', '2025-05-30 09:01:08.000000', 3, 6, 'nhjghfgvb', 'APPROVED', NULL, 22);

-- --------------------------------------------------------

--
-- Table structure for table `private_messages`
--

CREATE TABLE `private_messages` (
  `is_read` bit(1) NOT NULL,
  `id` bigint(20) NOT NULL,
  `receiver_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `attachment_original_filename` varchar(255) DEFAULT NULL,
  `attachment_path` varchar(255) DEFAULT NULL,
  `content` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `private_messages`
--

INSERT INTO `private_messages` (`is_read`, `id`, `receiver_id`, `sender_id`, `timestamp`, `attachment_original_filename`, `attachment_path`, `content`) VALUES
(b'0', 1, 3, 6, '2025-05-06 10:50:04.000000', NULL, NULL, 'hello'),
(b'0', 2, 3, 6, '2025-05-06 10:50:09.000000', NULL, NULL, 'laoshi'),
(b'0', 3, 6, 3, '2025-05-06 10:50:18.000000', NULL, NULL, 'hi'),
(b'0', 4, 3, 6, '2025-05-30 08:23:20.000000', NULL, NULL, 'dd');

-- --------------------------------------------------------

--
-- Table structure for table `questions`
--

CREATE TABLE `questions` (
  `id` bigint(20) NOT NULL,
  `points` int(11) NOT NULL,
  `question_order` int(11) NOT NULL,
  `question_text` text NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','MULTIPLE_SELECT') NOT NULL,
  `quiz_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quizzes`
--

CREATE TABLE `quizzes` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text DEFAULT NULL,
  `due_date` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `class_id` bigint(20) NOT NULL,
  `time_limit_minutes` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quizzes`
--

INSERT INTO `quizzes` (`id`, `created_at`, `description`, `due_date`, `title`, `class_id`, `time_limit_minutes`) VALUES
(1, '2025-04-28 12:38:42.000000', '', NULL, '22', 2, NULL),
(2, '2025-04-30 11:33:07.000000', 'do it !', NULL, 'Quiz1', 8, NULL),
(3, '2025-05-03 16:52:34.000000', '', NULL, 'QUiz1', 8, NULL),
(4, '2025-05-04 07:18:12.000000', '', NULL, 'Quiz1', 9, NULL),
(5, '2025-05-06 08:17:20.000000', '', NULL, 'quiz1', 5, NULL),
(6, '2025-05-06 08:25:36.000000', '', NULL, 'quiz2', 5, NULL),
(7, '2025-05-06 10:07:53.000000', '', '2025-05-15 10:07:00.000000', 'Quiz1', 6, NULL),
(8, '2025-05-06 10:22:30.000000', '', '2025-05-29 22:25:00.000000', 'Quiz2', 6, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `quiz_answers`
--

CREATE TABLE `quiz_answers` (
  `id` bigint(20) NOT NULL,
  `answer_text` text DEFAULT NULL,
  `points_awarded` double DEFAULT NULL,
  `was_correct` bit(1) DEFAULT NULL,
  `question_id` bigint(20) NOT NULL,
  `quiz_attempt_id` bigint(20) NOT NULL,
  `selected_option_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quiz_answers`
--

INSERT INTO `quiz_answers` (`id`, `answer_text`, `points_awarded`, `was_correct`, `question_id`, `quiz_attempt_id`, `selected_option_id`) VALUES
(15, '2', 98, NULL, 11, 1, NULL),
(16, '2', 86, NULL, 11, 3, NULL),
(17, '4', 70, NULL, 12, 4, NULL),
(18, '0', 1, NULL, 14, 6, NULL),
(19, '0\r\n', 100, NULL, 15, 5, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `quiz_answer_selected_answers`
--

CREATE TABLE `quiz_answer_selected_answers` (
  `quiz_answer_id` bigint(20) NOT NULL,
  `selected_answer_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_attempts`
--

CREATE TABLE `quiz_attempts` (
  `max_score` int(11) DEFAULT NULL,
  `score` double DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `quiz_id` bigint(20) NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `status` enum('GRADED','IN_PROGRESS','SUBMITTED') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quiz_attempts`
--

INSERT INTO `quiz_attempts` (`max_score`, `score`, `end_time`, `id`, `quiz_id`, `start_time`, `student_id`, `status`) VALUES
(100, 98, '2025-05-06 08:17:33.000000', 1, 5, '2025-05-06 08:17:28.000000', 4, 'GRADED'),
(NULL, NULL, NULL, 2, 6, '2025-05-06 08:36:29.000000', 4, 'IN_PROGRESS'),
(100, 86, '2025-05-06 08:52:00.000000', 3, 5, '2025-05-06 08:51:57.000000', 5, 'GRADED'),
(70, 70, '2025-05-06 08:54:26.000000', 4, 6, '2025-05-06 08:54:14.000000', 5, 'GRADED'),
(100, 100, '2025-05-06 10:44:37.000000', 5, 7, '2025-05-06 10:10:21.000000', 6, 'GRADED'),
(1, 1, '2025-05-06 10:38:25.000000', 6, 8, '2025-05-06 10:22:38.000000', 6, 'GRADED');

-- --------------------------------------------------------

--
-- Table structure for table `quiz_options`
--

CREATE TABLE `quiz_options` (
  `is_correct` bit(1) NOT NULL,
  `id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `option_text` varchar(1000) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_questions`
--

CREATE TABLE `quiz_questions` (
  `id` bigint(20) NOT NULL,
  `points` int(11) NOT NULL,
  `question_order` int(11) NOT NULL,
  `question_text` text NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','SHORT_ANSWER') NOT NULL,
  `quiz_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quiz_questions`
--

INSERT INTO `quiz_questions` (`id`, `points`, `question_order`, `question_text`, `question_type`, `quiz_id`) VALUES
(1, 55, 0, 'www', 'SHORT_ANSWER', 1),
(2, 45, 1, 'dd', 'SHORT_ANSWER', 1),
(3, 15, 0, '1+3', 'SHORT_ANSWER', 2),
(4, 15, 1, '2-3', 'SHORT_ANSWER', 2),
(5, 60, 2, '18-18', 'SHORT_ANSWER', 2),
(6, 100, 0, 'test', 'SHORT_ANSWER', 3),
(7, 20, 0, '1+9', 'SHORT_ANSWER', 4),
(8, 30, 1, '2+2', 'SHORT_ANSWER', 4),
(9, 30, 2, '8+8', 'SHORT_ANSWER', 4),
(10, 20, 3, '20+20', 'SHORT_ANSWER', 4),
(11, 100, 0, '1+1', 'SHORT_ANSWER', 5),
(12, 70, 0, '2+2', 'SHORT_ANSWER', 6),
(14, 1, 0, '2x+7=7\r\nx?', 'SHORT_ANSWER', 8),
(15, 100, 0, '2x+7=7\r\nfind x?', 'SHORT_ANSWER', 7);

-- --------------------------------------------------------

--
-- Table structure for table `roles`
--

CREATE TABLE `roles` (
  `id` bigint(20) NOT NULL,
  `name` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `roles`
--

INSERT INTO `roles` (`id`, `name`) VALUES
(1, 'ROLE_ADMIN'),
(3, 'ROLE_STUDENT'),
(2, 'ROLE_TEACHER');

-- --------------------------------------------------------

--
-- Table structure for table `submissions`
--

CREATE TABLE `submissions` (
  `is_makeup_submission` bit(1) NOT NULL,
  `numerical_grade` double DEFAULT NULL,
  `assignment_id` bigint(20) NOT NULL,
  `fulfilled_makeup_request_id` bigint(20) DEFAULT NULL,
  `graded_date` datetime(6) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `submission_date` datetime(6) NOT NULL,
  `grade` varchar(20) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `content_text` text DEFAULT NULL,
  `feedback` text DEFAULT NULL,
  `is_superseded` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `submissions`
--

INSERT INTO `submissions` (`is_makeup_submission`, `numerical_grade`, `assignment_id`, `fulfilled_makeup_request_id`, `graded_date`, `id`, `student_id`, `submission_date`, `grade`, `file_path`, `original_filename`, `content_text`, `feedback`, `is_superseded`) VALUES
(b'1', 88, 12, NULL, '2025-05-06 10:46:26.000000', 18, 6, '2025-05-06 10:02:56.000000', '88', NULL, NULL, 'assignment aswers!!!', '', b'0'),
(b'1', 14, 13, NULL, '2025-05-30 08:22:26.000000', 19, 6, '2025-05-30 08:21:23.000000', '14', '6_13_e3352470-bf0e-4faa-853b-99461d50b2de.jpg', 'بداية نهاية منح الصين آخر المنح الصينيية(4).jpg', 'hh', '', b'0'),
(b'1', 22, 14, NULL, '2025-05-30 08:35:27.000000', 20, 6, '2025-05-30 08:35:14.000000', '22', NULL, NULL, 'ccc', '', b'0'),
(b'1', 11, 16, NULL, '2025-05-30 08:53:08.000000', 21, 6, '2025-05-30 08:52:53.000000', '11', NULL, NULL, '1111111111111xxxxxxxxxxxx', '', b'0'),
(b'1', 59, 15, NULL, '2025-05-30 09:01:38.000000', 22, 6, '2025-05-30 09:01:20.000000', '59', NULL, NULL, 'wwwwmnmjgwwwwwww', '', b'0');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `enabled` bit(1) NOT NULL,
  `id` bigint(20) NOT NULL,
  `student_id` varchar(50) DEFAULT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `username` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `profile_picture_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`enabled`, `id`, `student_id`, `first_name`, `last_name`, `username`, `password`, `profile_picture_path`) VALUES
(b'1', 1, NULL, 'Admin', 'User', 'admin@admin.com', '$2a$10$sxYK1cHWSkvpD4hvoevpyOERsfMLm6px.1LYe/wqsvpPlIzUhssi.', NULL),
(b'1', 2, NULL, 'Admin', 'User', 'admin', '$2a$10$NPWgbFyBIrD0sy4CAyyPV.5cWfEcQWG2HIhkEr03XCQuROVFn0JIW', NULL),
(b'1', 3, NULL, 'fang', 'class', 'fang@dpu.com', '$2a$10$Albv8VwreW4VCcDInsh/rek5NNO7V1n4pd0ctiGGYd7Fp0u287e8W', 'fang@dpu.com_a3f4194b-3a9e-4903-a2e8-f3b404c2d4ab.jpg'),
(b'1', 4, '219J98', 'Hamza', 'El Massaoudy', 'elmassaoudy.hamza@gmail.com', '$2a$10$G2gGKRHgdx1oAmAxE9siOetcB3791a8R3KrMu9FP34tzfgBMl..pa', NULL),
(b'1', 5, '123123J123', '姆扎', '哈', 'thescriptxxwebsite@gmail.com', '$2a$10$.no654NuKH4B/ELJDbdYvu1yP1Q5JI.7KL/4ClAaxn/AXXHh.9TuK', NULL),
(b'1', 6, '213J14', 'Meriem', 'Guada', 'meriem@guada.com', '$2a$10$IzXnLUWl/FGi2JhAU49PEOQTunOziwVJG3qtdsLxKjdNVU98RF5Ca', 'meriem@guada.com_273d4898-4be5-47ff-a200-53d7ba1982f9.jpg'),
(b'1', 7, NULL, 'Admin', 'DPU', 'admin@dpu.com', '$2a$10$NWXftp0k7TPAPv6gHi1yQO6fl9k1t1Ds8PcBD4xd46bFFw6HgNUPa', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `users_roles`
--

CREATE TABLE `users_roles` (
  `role_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users_roles`
--

INSERT INTO `users_roles` (`role_id`, `user_id`) VALUES
(1, 1),
(1, 2),
(1, 7),
(2, 3),
(3, 4),
(3, 5),
(3, 6);

-- --------------------------------------------------------

--
-- Table structure for table `user_friends`
--

CREATE TABLE `user_friends` (
  `friend_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_friends`
--

INSERT INTO `user_friends` (`friend_id`, `user_id`) VALUES
(3, 6),
(6, 3);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `answers`
--
ALTER TABLE `answers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK3erw1a3t0r78st8ty27x6v3g1` (`question_id`);

--
-- Indexes for table `assignments`
--
ALTER TABLE `assignments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK15vvra255mh2h6merxffxmb2t` (`class_id`);

--
-- Indexes for table `chat_messages`
--
ALTER TABLE `chat_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKowvesxqch0emsw8xwsb73w2cb` (`class_id`),
  ADD KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`);

--
-- Indexes for table `classes`
--
ALTER TABLE `classes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKo7h6axo2jyskq2aqusb5povfp` (`class_code`),
  ADD KEY `FK4tv5efpqhlo8xg8l8dpba8v75` (`teacher_id`);

--
-- Indexes for table `classes_students`
--
ALTER TABLE `classes_students`
  ADD PRIMARY KEY (`class_id`,`student_id`),
  ADD KEY `FK8ua8ibh49fd0xyv6vsj9qhgcl` (`student_id`);

--
-- Indexes for table `friend_requests`
--
ALTER TABLE `friend_requests`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_friend_request_sender_receiver` (`sender_id`,`receiver_id`),
  ADD KEY `FKtcmqalc5v4qdt1slgcsa544i5` (`receiver_id`);

--
-- Indexes for table `makeup_requests`
--
ALTER TABLE `makeup_requests`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKpayq96529wixq4fnhyurlvliq` (`original_submission_id`),
  ADD UNIQUE KEY `UKmvbao6vr19j7y5lbeqld0axu8` (`submission_id`),
  ADD KEY `FK1eionvun3gibvye0r4y8c9imk` (`reviewed_by_teacher_id`),
  ADD KEY `FKq80kp8836gl6wl9polqv59bin` (`student_id`);

--
-- Indexes for table `private_messages`
--
ALTER TABLE `private_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKohfx6x8sqhg2mehdc6pnadejw` (`receiver_id`),
  ADD KEY `FK1kix63i73ln942n697wudh9sj` (`sender_id`);

--
-- Indexes for table `questions`
--
ALTER TABLE `questions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKn3gvco4b0kewxc0bywf1igfms` (`quiz_id`);

--
-- Indexes for table `quizzes`
--
ALTER TABLE `quizzes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKaq63onrahh6ox7q2fdadur4r5` (`class_id`);

--
-- Indexes for table `quiz_answers`
--
ALTER TABLE `quiz_answers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKb69mwpkm3kehim0klscpmmkc1` (`question_id`),
  ADD KEY `FKlcy4hia77u855vdmv9tdiuuo4` (`selected_option_id`),
  ADD KEY `FKavoae3u6jysuq41yn5t9umbdv` (`quiz_attempt_id`);

--
-- Indexes for table `quiz_answer_selected_answers`
--
ALTER TABLE `quiz_answer_selected_answers`
  ADD PRIMARY KEY (`quiz_answer_id`,`selected_answer_id`),
  ADD KEY `FK9706yewn18t00q1g0dauw8dgy` (`selected_answer_id`);

--
-- Indexes for table `quiz_attempts`
--
ALTER TABLE `quiz_attempts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKfwipvfipnnwsoacoyv5k7fbxc` (`quiz_id`),
  ADD KEY `FKl6lkk2u7vw7q7kw3udhupe7ut` (`student_id`);

--
-- Indexes for table `quiz_options`
--
ALTER TABLE `quiz_options`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKhkuvmd7qk1lmtq3hy9htpinkr` (`question_id`);

--
-- Indexes for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKanfmgf6ksbdnv7ojb0pfve54q` (`quiz_id`);

--
-- Indexes for table `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`);

--
-- Indexes for table `submissions`
--
ALTER TABLE `submissions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK1g4vf2isekr4ntt5v7etwji7r` (`fulfilled_makeup_request_id`),
  ADD KEY `FKrirbb44savy2g7nws0hoxs949` (`assignment_id`),
  ADD KEY `FK3p6y8mnhpwusdgqrdl4hcl72m` (`student_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_user_username` (`username`),
  ADD UNIQUE KEY `uk_user_studentid` (`student_id`);

--
-- Indexes for table `users_roles`
--
ALTER TABLE `users_roles`
  ADD PRIMARY KEY (`role_id`,`user_id`),
  ADD KEY `FK2o0jvgh89lemvvo17cbqvdxaa` (`user_id`);

--
-- Indexes for table `user_friends`
--
ALTER TABLE `user_friends`
  ADD PRIMARY KEY (`friend_id`,`user_id`),
  ADD KEY `FKk08ugelrh9cea1oew3hgxryw2` (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `answers`
--
ALTER TABLE `answers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `assignments`
--
ALTER TABLE `assignments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `chat_messages`
--
ALTER TABLE `chat_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `classes`
--
ALTER TABLE `classes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `friend_requests`
--
ALTER TABLE `friend_requests`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `makeup_requests`
--
ALTER TABLE `makeup_requests`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `private_messages`
--
ALTER TABLE `private_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `questions`
--
ALTER TABLE `questions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quizzes`
--
ALTER TABLE `quizzes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `quiz_answers`
--
ALTER TABLE `quiz_answers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `quiz_attempts`
--
ALTER TABLE `quiz_attempts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `quiz_options`
--
ALTER TABLE `quiz_options`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `roles`
--
ALTER TABLE `roles`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `submissions`
--
ALTER TABLE `submissions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `answers`
--
ALTER TABLE `answers`
  ADD CONSTRAINT `FK3erw1a3t0r78st8ty27x6v3g1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`);

--
-- Constraints for table `assignments`
--
ALTER TABLE `assignments`
  ADD CONSTRAINT `FK15vvra255mh2h6merxffxmb2t` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`);

--
-- Constraints for table `chat_messages`
--
ALTER TABLE `chat_messages`
  ADD CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKowvesxqch0emsw8xwsb73w2cb` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`);

--
-- Constraints for table `classes`
--
ALTER TABLE `classes`
  ADD CONSTRAINT `FK4tv5efpqhlo8xg8l8dpba8v75` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `classes_students`
--
ALTER TABLE `classes_students`
  ADD CONSTRAINT `FK2u9dskkphqycj6op1b0c8b6uf` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  ADD CONSTRAINT `FK8ua8ibh49fd0xyv6vsj9qhgcl` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `friend_requests`
--
ALTER TABLE `friend_requests`
  ADD CONSTRAINT `FKcchlh48b4347amfvmke793bg7` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKtcmqalc5v4qdt1slgcsa544i5` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `makeup_requests`
--
ALTER TABLE `makeup_requests`
  ADD CONSTRAINT `FK1eionvun3gibvye0r4y8c9imk` FOREIGN KEY (`reviewed_by_teacher_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKbdjpdc5f4tca4mfbanif5jc3c` FOREIGN KEY (`original_submission_id`) REFERENCES `submissions` (`id`),
  ADD CONSTRAINT `FKq80kp8836gl6wl9polqv59bin` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKruxgas9m7ue7dkuaujbf06mdt` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`);

--
-- Constraints for table `private_messages`
--
ALTER TABLE `private_messages`
  ADD CONSTRAINT `FK1kix63i73ln942n697wudh9sj` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKohfx6x8sqhg2mehdc6pnadejw` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `questions`
--
ALTER TABLE `questions`
  ADD CONSTRAINT `FKn3gvco4b0kewxc0bywf1igfms` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`);

--
-- Constraints for table `quiz_answers`
--
ALTER TABLE `quiz_answers`
  ADD CONSTRAINT `FK_quiz_answers_question` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`),
  ADD CONSTRAINT `FKavoae3u6jysuq41yn5t9umbdv` FOREIGN KEY (`quiz_attempt_id`) REFERENCES `quiz_attempts` (`id`),
  ADD CONSTRAINT `FKb69mwpkm3kehim0klscpmmkc1` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`),
  ADD CONSTRAINT `FKlcy4hia77u855vdmv9tdiuuo4` FOREIGN KEY (`selected_option_id`) REFERENCES `quiz_options` (`id`);

--
-- Constraints for table `quiz_answer_selected_answers`
--
ALTER TABLE `quiz_answer_selected_answers`
  ADD CONSTRAINT `FK9706yewn18t00q1g0dauw8dgy` FOREIGN KEY (`selected_answer_id`) REFERENCES `answers` (`id`),
  ADD CONSTRAINT `FKh7ubohmh9gw5ogvitfy5wha4g` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answers` (`id`);

--
-- Constraints for table `quiz_attempts`
--
ALTER TABLE `quiz_attempts`
  ADD CONSTRAINT `FKfwipvfipnnwsoacoyv5k7fbxc` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`),
  ADD CONSTRAINT `FKl6lkk2u7vw7q7kw3udhupe7ut` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `quiz_options`
--
ALTER TABLE `quiz_options`
  ADD CONSTRAINT `FKhkuvmd7qk1lmtq3hy9htpinkr` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`);

--
-- Constraints for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD CONSTRAINT `FKanfmgf6ksbdnv7ojb0pfve54q` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`);

--
-- Constraints for table `submissions`
--
ALTER TABLE `submissions`
  ADD CONSTRAINT `FK3p6y8mnhpwusdgqrdl4hcl72m` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKn5oeoeqaw1few6k4u9pdpg378` FOREIGN KEY (`fulfilled_makeup_request_id`) REFERENCES `makeup_requests` (`id`),
  ADD CONSTRAINT `FKrirbb44savy2g7nws0hoxs949` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`);

--
-- Constraints for table `users_roles`
--
ALTER TABLE `users_roles`
  ADD CONSTRAINT `FK2o0jvgh89lemvvo17cbqvdxaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKj6m8fwv7oqv74fcehir1a9ffy` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`);

--
-- Constraints for table `user_friends`
--
ALTER TABLE `user_friends`
  ADD CONSTRAINT `FK11y5boh1e7gh60rdqixyetv3x` FOREIGN KEY (`friend_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKk08ugelrh9cea1oew3hgxryw2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
