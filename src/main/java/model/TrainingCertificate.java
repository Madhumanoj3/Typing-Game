package model;

import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class TrainingCertificate {

    private ObjectId id;
    private String username;
    private String status;
    private String grade;
    private double averageWpm;
    private double averageAccuracy;
    private int completedLessons;
    private int totalLessons;
    private LocalDateTime requestedAt;
    private LocalDateTime gradedAt;
    private String gradedBy;
    private String certificatePath;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public double getAverageWpm() { return averageWpm; }
    public void setAverageWpm(double averageWpm) { this.averageWpm = averageWpm; }

    public double getAverageAccuracy() { return averageAccuracy; }
    public void setAverageAccuracy(double averageAccuracy) { this.averageAccuracy = averageAccuracy; }

    public int getCompletedLessons() { return completedLessons; }
    public void setCompletedLessons(int completedLessons) { this.completedLessons = completedLessons; }

    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }

    public String getGradedBy() { return gradedBy; }
    public void setGradedBy(String gradedBy) { this.gradedBy = gradedBy; }

    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
}
