CREATE DATABASE IF NOT EXISTS facback;
USE facback;

CREATE TABLE IF NOT EXISTS Teachers (
  teacher_id INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(60) NOT NULL,
  dept       VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS Courses (
  course_id  INT AUTO_INCREMENT PRIMARY KEY,
  teacher_id INT NOT NULL,
  code       VARCHAR(15) NOT NULL,
  name       VARCHAR(80) NOT NULL,
  UNIQUE (teacher_id, code),
  FOREIGN KEY (teacher_id) REFERENCES Teachers(teacher_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Users (
  username   VARCHAR(30) PRIMARY KEY,
  password   VARCHAR(30) COLLATE utf8mb4_bin NOT NULL,
  role       ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL,
  teacher_id INT NULL,
  last_seen_review INT NOT NULL DEFAULT 0,
  FOREIGN KEY (teacher_id) REFERENCES Teachers(teacher_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Submissions (
  hash           CHAR(64) PRIMARY KEY,
  last_submitted DATE     NOT NULL
);

CREATE TABLE IF NOT EXISTS Reviews (
  review_id     INT AUTO_INCREMENT PRIMARY KEY,
  course_id     INT NOT NULL,
  clarity       INT NOT NULL CHECK (clarity BETWEEN 1 AND 5),
  punctuality   INT NOT NULL CHECK (punctuality BETWEEN 1 AND 5),
  doubt_solving INT NOT NULL CHECK (doubt_solving BETWEEN 1 AND 5),
  overall       INT NOT NULL CHECK (overall BETWEEN 1 AND 5),
  comment       VARCHAR(300),
  review_month  CHAR(7) NOT NULL,
  FOREIGN KEY (course_id) REFERENCES Courses(course_id) ON DELETE CASCADE
);

-- PBL-2: sections, a named student roster, marks, attendance, and the FA outcome log.
-- Reviews stay anonymous; nothing below links a student to a review.

CREATE TABLE IF NOT EXISTS Sections (
  section_id INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(20) NOT NULL UNIQUE,
  fa_teacher INT NULL,
  FOREIGN KEY (fa_teacher) REFERENCES Teachers(teacher_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS Students (
  student_id INT AUTO_INCREMENT PRIMARY KEY,
  reg_no     VARCHAR(15) NOT NULL UNIQUE,
  name       VARCHAR(60) NOT NULL,
  section_id INT NOT NULL,
  FOREIGN KEY (section_id) REFERENCES Sections(section_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Tests (
  test_id   INT AUTO_INCREMENT PRIMARY KEY,
  course_id INT NOT NULL,
  label     VARCHAR(30) NOT NULL,
  max_marks DECIMAL(6,2) NOT NULL,
  held_on   DATE NULL,
  UNIQUE (course_id, label),
  FOREIGN KEY (course_id) REFERENCES Courses(course_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Marks (
  student_id INT NOT NULL,
  test_id    INT NOT NULL,
  score      DECIMAL(6,2) NOT NULL,
  PRIMARY KEY (student_id, test_id),
  FOREIGN KEY (student_id) REFERENCES Students(student_id) ON DELETE CASCADE,
  FOREIGN KEY (test_id)    REFERENCES Tests(test_id)       ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Attendance (
  student_id INT NOT NULL,
  course_id  INT NOT NULL,
  class_date DATE NOT NULL,
  present    BOOLEAN NOT NULL,
  PRIMARY KEY (student_id, course_id, class_date),
  FOREIGN KEY (student_id) REFERENCES Students(student_id) ON DELETE CASCADE,
  FOREIGN KEY (course_id)  REFERENCES Courses(course_id)   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS FaOutcomes (
  outcome_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  flag_kind  VARCHAR(40) NOT NULL,
  outcome    VARCHAR(40) NOT NULL,
  note       VARCHAR(300),
  logged_on  DATE NOT NULL,
  FOREIGN KEY (student_id) REFERENCES Students(student_id) ON DELETE CASCADE
);

INSERT IGNORE INTO Teachers (teacher_id, name, dept) VALUES
  (1, 'Faculty A', 'CSE (Data Science)'), (2, 'Faculty B', 'CSE (Data Science)'),
  (3, 'Faculty C', 'CSE (Core)'),         (4, 'Faculty D', 'ECE'),
  (5, 'Faculty E', 'Mechanical'),         (6, 'Faculty F', 'CSE (Data Science)');
INSERT IGNORE INTO Courses (course_id, teacher_id, code, name) VALUES
  (1, 2, '21CSC206P', 'Advanced Object Oriented Programming'),
  (2, 1, '21CSC201J', 'Data Structures and Algorithms'),
  (3, 6, '21CSS202T', 'Fundamentals of Data Science'),
  (4, 3, '21CSC202J', 'Operating Systems'),
  (5, 4, '21ECC201T', 'Electronic Devices'),
  (6, 5, '21MEC201T', 'Engineering Thermodynamics');
INSERT IGNORE INTO Users (username, password, role, teacher_id) VALUES
  ('admin', 'admin123', 'ADMIN', NULL),
  ('facultyA', 'pass', 'TEACHER', 1), ('facultyB', 'pass', 'TEACHER', 2),
  ('facultyC', 'pass', 'TEACHER', 3), ('facultyD', 'pass', 'TEACHER', 4),
  ('facultyE', 'pass', 'TEACHER', 5), ('facultyF', 'pass', 'TEACHER', 6);
INSERT IGNORE INTO Sections (section_id, name, fa_teacher) VALUES
  (1, 'AS1', NULL);
