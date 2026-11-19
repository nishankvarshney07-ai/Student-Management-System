package com.example.student.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Student {
    private int studentId;
    private String studentName;
    private String emailAddress;
    private String courseName;
    private double programmingScore;
    private double mathematicsScore;
    private List<String> technicalSkills = new ArrayList<>();

    public Student() {
    }

    public Student(int studentId, String studentName, String emailAddress,
                   String courseName, double programmingScore, double mathematicsScore,
                   List<String> technicalSkills) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.emailAddress = emailAddress;
        this.courseName = courseName;
        setProgrammingScore(programmingScore);
        setMathematicsScore(mathematicsScore);
        if (technicalSkills != null) {
            this.technicalSkills = new ArrayList<>(technicalSkills);
        }
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public double getProgrammingScore() { return programmingScore; }
    public void setProgrammingScore(double programmingScore) {
        if (programmingScore < 0 || programmingScore > 100) {
            throw new IllegalArgumentException("Programming score must be between 0 and 100.");
        }
        this.programmingScore = programmingScore;
    }

    public double getMathematicsScore() { return mathematicsScore; }
    public void setMathematicsScore(double mathematicsScore) {
        if (mathematicsScore < 0 || mathematicsScore > 100) {
            throw new IllegalArgumentException("Mathematics score must be between 0 and 100.");
        }
        this.mathematicsScore = mathematicsScore;
    }

    public List<String> getTechnicalSkills() { return technicalSkills; }
    public void setTechnicalSkills(List<String> technicalSkills) {
        this.technicalSkills = (technicalSkills != null) ? new ArrayList<>(technicalSkills) : new ArrayList<>();
    }

    public double getAverageScore() {
        return (programmingScore + mathematicsScore) / 2.0;
    }

    public String getPerformanceCategory() {
        double avg = getAverageScore();
        if (avg >= 85.0) return "Excellent";
        if (avg >= 70.0) return "Good";
        if (avg >= 50.0) return "Satisfactory";
        return "Needs Improvement";
    }

    public String getStrongerSubject() {
        if (programmingScore > mathematicsScore) return "Programming";
        if (mathematicsScore > programmingScore) return "Mathematics";
        return "Both Equal";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return studentId == student.studentId && Objects.equals(emailAddress, student.emailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, emailAddress);
    }
}
