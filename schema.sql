CREATE DATABASE IF NOT EXISTS student_db;
USE student_db;

DROP TABLE IF EXISTS student_skills;
DROP TABLE IF EXISTS students;

CREATE TABLE students (
    student_id INT PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(120) NOT NULL UNIQUE,
    course_name VARCHAR(80) NOT NULL,
    programming_score DOUBLE NOT NULL CHECK (programming_score >= 0 AND programming_score <= 100),
    mathematics_score DOUBLE NOT NULL CHECK (mathematics_score >= 0 AND mathematics_score <= 100),
    average_score DOUBLE GENERATED ALWAYS AS ((programming_score + mathematics_score) / 2.0) STORED,
    performance_category VARCHAR(30) GENERATED ALWAYS AS (
        CASE
            WHEN ((programming_score + mathematics_score) / 2.0) >= 85 THEN 'Excellent'
            WHEN ((programming_score + mathematics_score) / 2.0) >= 70 THEN 'Good'
            WHEN ((programming_score + mathematics_score) / 2.0) >= 50 THEN 'Satisfactory'
            ELSE 'Needs Improvement'
        END
    ) STORED,
    stronger_subject VARCHAR(20) GENERATED ALWAYS AS (
        CASE
            WHEN programming_score > mathematics_score THEN 'Programming'
            WHEN mathematics_score > programming_score THEN 'Mathematics'
            ELSE 'Both Equal'
        END
    ) STORED
);

CREATE TABLE student_skills (
    skill_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    skill_name VARCHAR(60) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_skill (student_id, skill_name)
);

INSERT INTO students (student_id, student_name, email_address, course_name, programming_score, mathematics_score) VALUES
(101, 'Nishank Varshney', 'nishank@example.com', 'B.Tech IT', 92.5, 88.0),
(102, 'Aarav Sharma', 'aarav@example.com', 'B.Tech CSE', 78.0, 84.0),
(103, 'Priya Verma', 'priya@example.com', 'B.Tech IT', 45.0, 82.0),
(104, 'Rohan Gupta', 'rohan@example.com', 'B.Tech ECE', 62.0, 58.0),
(105, 'Ananya Singh', 'ananya@example.com', 'B.Tech CSE', 42.0, 46.0);

INSERT INTO student_skills (student_id, skill_name) VALUES
(101, 'Java'),
(101, 'Spring Boot'),
(102, 'Java'),
(102, 'Python'),
(103, 'Python'),
(104, 'C++');
