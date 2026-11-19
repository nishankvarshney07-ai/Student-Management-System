package com.example.student.controller;

import com.example.student.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private DataSource dataSource;

    private List<String> getSkills(Connection conn, int studentId) throws SQLException {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT skill_name FROM student_skills WHERE student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getString("skill_name"));
                }
            }
        }
        return skills;
    }

    private Student mapRow(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("student_id");
        return new Student(
                id,
                rs.getString("student_name"),
                rs.getString("email_address"),
                rs.getString("course_name"),
                rs.getDouble("programming_score"),
                rs.getDouble("mathematics_score"),
                getSkills(conn, id)
        );
    }

    @PostMapping
    public ResponseEntity<?> addStudent(@RequestBody Student student) {
        if (student.getProgrammingScore() < 0 || student.getProgrammingScore() > 100 ||
            student.getMathematicsScore() < 0 || student.getMathematicsScore() > 100) {
            return new ResponseEntity<>("Scores must be between 0 and 100.", HttpStatus.BAD_REQUEST);
        }

        String insertStudentSql = "INSERT INTO students (student_id, student_name, email_address, course_name, programming_score, mathematics_score) VALUES (?, ?, ?, ?, ?, ?)";
        String insertSkillSql = "INSERT IGNORE INTO student_skills (student_id, skill_name) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psStudent = conn.prepareStatement(insertStudentSql);
                 PreparedStatement psSkill = conn.prepareStatement(insertSkillSql)) {

                psStudent.setInt(1, student.getStudentId());
                psStudent.setString(2, student.getStudentName());
                psStudent.setString(3, student.getEmailAddress());
                psStudent.setString(4, student.getCourseName());
                psStudent.setDouble(5, student.getProgrammingScore());
                psStudent.setDouble(6, student.getMathematicsScore());
                psStudent.executeUpdate();

                if (student.getTechnicalSkills() != null) {
                    for (String skill : student.getTechnicalSkills()) {
                        if (skill != null && !skill.trim().isEmpty()) {
                            psSkill.setInt(1, student.getStudentId());
                            psSkill.setString(2, skill.trim());
                            psSkill.addBatch();
                        }
                    }
                    psSkill.executeBatch();
                }
                conn.commit();
                return new ResponseEntity<>(student, HttpStatus.CREATED);
            } catch (SQLIntegrityConstraintViolationException dup) {
                conn.rollback();
                return new ResponseEntity<>("Duplicate Student ID or Email already exists.", HttpStatus.CONFLICT);
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY student_id ASC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(conn, rs));
            }
            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable int id) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ResponseEntity<>(mapRow(conn, rs), HttpStatus.OK);
                }
                return new ResponseEntity<>("Student not found with ID: " + id, HttpStatus.NOT_FOUND);
            }
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateScoresAndSkills(@PathVariable int id, @RequestBody Student updatedData) {
        if (updatedData.getProgrammingScore() < 0 || updatedData.getProgrammingScore() > 100 ||
            updatedData.getMathematicsScore() < 0 || updatedData.getMathematicsScore() > 100) {
            return new ResponseEntity<>("Scores must be between 0 and 100.", HttpStatus.BAD_REQUEST);
        }

        String updateSql = "UPDATE students SET programming_score = ?, mathematics_score = ? WHERE student_id = ?";
        String deleteSkillsSql = "DELETE FROM student_skills WHERE student_id = ?";
        String insertSkillSql = "INSERT INTO student_skills (student_id, skill_name) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setDouble(1, updatedData.getProgrammingScore());
                psUpdate.setDouble(2, updatedData.getMathematicsScore());
                psUpdate.setInt(3, id);

                int rows = psUpdate.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return new ResponseEntity<>("Student not found with ID: " + id, HttpStatus.NOT_FOUND);
                }

                if (updatedData.getTechnicalSkills() != null) {
                    try (PreparedStatement psDel = conn.prepareStatement(deleteSkillsSql);
                         PreparedStatement psIns = conn.prepareStatement(insertSkillSql)) {
                        psDel.setInt(1, id);
                        psDel.executeUpdate();

                        for (String skill : updatedData.getTechnicalSkills()) {
                            if (skill != null && !skill.trim().isEmpty()) {
                                psIns.setInt(1, id);
                                psIns.setString(2, skill.trim());
                                psIns.addBatch();
                            }
                        }
                        psIns.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
            return getStudentById(id);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable int id) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                return new ResponseEntity<>("Student deleted successfully.", HttpStatus.OK);
            }
            return new ResponseEntity<>("Student not found with ID: " + id, HttpStatus.NOT_FOUND);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/merit-list")
    public ResponseEntity<?> getMeritList() {
        List<Student> meritList = new ArrayList<>();
        String sql = "SELECT *, ((programming_score + mathematics_score) / 2.0) AS avg_score FROM students ORDER BY avg_score DESC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                meritList.add(mapRow(conn, rs));
            }
            return new ResponseEntity<>(meritList, HttpStatus.OK);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchByEmailOrCourse(@RequestParam(required = false) String email,
                                                   @RequestParam(required = false) String course) {
        List<Student> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps;
            if (email != null) {
                ps = conn.prepareStatement("SELECT * FROM students WHERE email_address = ?");
                ps.setString(1, email);
            } else if (course != null) {
                ps = conn.prepareStatement("SELECT * FROM students WHERE LOWER(course_name) = LOWER(?)");
                ps.setString(1, course);
            } else {
                return new ResponseEntity<>("Provide either 'email' or 'course' query parameter.", HttpStatus.BAD_REQUEST);
            }
            try (ps; ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(conn, rs));
                }
            }
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/shared-skills")
    public ResponseEntity<?> getSharedSkills() {
        String sql = "SELECT sk.skill_name, s.student_id, s.student_name " +
                     "FROM student_skills sk JOIN students s ON sk.student_id = s.student_id " +
                     "WHERE LOWER(sk.skill_name) IN (" +
                     "  SELECT LOWER(skill_name) FROM student_skills GROUP BY LOWER(skill_name) HAVING COUNT(DISTINCT student_id) > 1" +
                     ") ORDER BY sk.skill_name, s.student_id";
        Map<String, List<String>> shared = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                shared.computeIfAbsent(rs.getString("skill_name"), k -> new ArrayList<>())
                      .add(rs.getString("student_name") + " (ID: " + rs.getInt("student_id") + ")");
            }
            return new ResponseEntity<>(shared, HttpStatus.OK);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/math-strong-prog-weak")
    public ResponseEntity<?> getMathStrongProgWeak() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE programming_score < 50 AND mathematics_score >= 75";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(conn, rs));
            }
            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (SQLException e) {
            return new ResponseEntity<>("Database Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
