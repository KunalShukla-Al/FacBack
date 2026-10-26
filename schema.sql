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
