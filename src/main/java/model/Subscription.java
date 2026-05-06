package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class Subscription {

    private ObjectId      id;
    private String        username;
    private String        plan;      // "FREE" | "MONTHLY" | "LIFETIME"
    private String        status;    // "ACTIVE" | "EXPIRED" | "CANCELLED"
    private LocalDateTime startDate;
    private LocalDateTime endDate;   // null means no expiry (LIFETIME)
    private String        paymentMethod; // "CARD" | "UPI"
    private String        paymentDetail; // masked card / UPI id for admin and bill
    private LocalDateTime verifiedDate;
    private String        billPath;

    public Subscription() {}

    public Subscription(String username, String plan, String status,
                        LocalDateTime startDate, LocalDateTime endDate) {
        this.username  = username;
        this.plan      = plan;
        this.status    = status;
        this.startDate = startDate;
        this.endDate   = endDate;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                 { return id; }
    public void setId(ObjectId id)          { this.id = id; }

    public String getUsername()             { return username; }
    public void setUsername(String v)       { this.username = v; }

    public String getPlan()                 { return plan; }
    public void setPlan(String v)           { this.plan = v; }

    public String getStatus()               { return status; }
    public void setStatus(String v)         { this.status = v; }

    public LocalDateTime getStartDate()     { return startDate; }
    public void setStartDate(LocalDateTime v) { this.startDate = v; }

    public LocalDateTime getEndDate()       { return endDate; }
    public void setEndDate(LocalDateTime v) { this.endDate = v; }

    public String getPaymentMethod()        { return paymentMethod; }
    public void setPaymentMethod(String v)  { this.paymentMethod = v; }

    public String getPaymentDetail()        { return paymentDetail; }
    public void setPaymentDetail(String v)  { this.paymentDetail = v; }

    public LocalDateTime getVerifiedDate()  { return verifiedDate; }
    public void setVerifiedDate(LocalDateTime v) { this.verifiedDate = v; }

    public String getBillPath()             { return billPath; }
    public void setBillPath(String v)       { this.billPath = v; }

    public boolean isActive() {
        if (!"ACTIVE".equals(status)) return false;
        if (endDate == null) return true;               // LIFETIME
        return endDate.isAfter(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("Subscription{user='%s', plan='%s', status='%s'}", username, plan, status);
    }
}
