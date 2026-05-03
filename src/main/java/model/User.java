package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class User {

    private ObjectId id;
    private String phone;
    private String address;
    private int age;
    private String dob;
    private String username;
    private String email;
    private String passwordHash;
    private int totalGames;
    private double bestWpm;
    private double averageWpm;
    private double bestAccuracy;
    private LocalDateTime createdAt;

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String phone, String address, int age, String dob, String username, String email, String passwordHash) {
        this();
        this.phone        = phone;
        this.address      = address;
        this.age          = age;
        this.dob          = dob;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.totalGames   = 0;
        this.bestWpm      = 0.0;
        this.averageWpm   = 0.0;
        this.bestAccuracy = 0.0;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                { return id; }
    public void setId(ObjectId id)         { this.id = id; }

    public String getPhone()               { return phone; }
    public void setPhone(String v)         { this.phone = v; }

    public String getAddress()             { return address; }
    public void setAddress(String v)       { this.address = v; }

    public int getAge()                    { return age; }
    public void setAge(int v)              { this.age = v; }

    public String getDob()                 { return dob; }
    public void setDob(String v)           { this.dob = v; }

    public String getUsername()            { return username; }
    public void setUsername(String v)      { this.username = v; }

    public String getEmail()               { return email; }
    public void setEmail(String v)        { this.email = v; }

    public String getPasswordHash()        { return passwordHash; }
    public void setPasswordHash(String v)  { this.passwordHash = v; }

    public int getTotalGames()             { return totalGames; }
    public void setTotalGames(int v)       { this.totalGames = v; }

    public double getBestWpm()             { return bestWpm; }
    public void setBestWpm(double v)       { this.bestWpm = v; }

    public double getAverageWpm()          { return averageWpm; }
    public void setAverageWpm(double v)    { this.averageWpm = v; }

    public double getBestAccuracy()        { return bestAccuracy; }
    public void setBestAccuracy(double v)  { this.bestAccuracy = v; }

    public LocalDateTime getCreatedAt()    { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    @Override
    public String toString() {
        return "User{username='" + username + "', email='" + email + "', bestWpm=" + bestWpm + "}";
    }
}
