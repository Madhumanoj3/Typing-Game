package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import model.Subscription;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Data access for the {@code subscriptions} collection.
 */
public class SubscriptionDAO {

    private static SubscriptionDAO instance;
    private final MongoCollection<Document> col;

    private SubscriptionDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("subscriptions");
    }

    public static SubscriptionDAO getInstance() {
        if (instance == null) instance = new SubscriptionDAO();
        return instance;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    public void save(Subscription sub) {
        Document doc = new Document()
                .append("username",  sub.getUsername())
                .append("plan",      sub.getPlan())
                .append("status",    sub.getStatus())
                .append("startDate", toDate(sub.getStartDate()))
                .append("endDate",   sub.getEndDate() != null ? toDate(sub.getEndDate()) : null)
                .append("paymentMethod", sub.getPaymentMethod())
                .append("paymentDetail", sub.getPaymentDetail())
                .append("verifiedDate", sub.getVerifiedDate() != null ? toDate(sub.getVerifiedDate()) : null)
                .append("billPath", sub.getBillPath());
        col.insertOne(doc);
        sub.setId(doc.getObjectId("_id"));
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /** Returns the most recent subscription record for a user, or null if none. */
    public Subscription getLatest(String username) {
        Document d = col.find(Filters.eq("username", username))
                        .sort(Sorts.descending("startDate"))
                        .first();
        return d == null ? null : toSub(d);
    }

    /** Returns true if the user currently has an active premium subscription. */
    public boolean isPremium(String username) {
        Subscription sub = getLatest(username);
        return sub != null && sub.isActive() && !"FREE".equals(sub.getPlan());
    }

    // ── Admin Operations ──────────────────────────────────────────────────

    /** Returns all subscription records. */
    public java.util.List<Subscription> getAllSubscriptions() {
        java.util.List<Subscription> list = new java.util.ArrayList<>();
        for (Document d : col.find().sort(Sorts.descending("startDate"))) {
            list.add(toSub(d));
        }
        return list;
    }

    /** Updates subscription status (ACTIVE / EXPIRED / CANCELLED). */
    public void updateStatus(org.bson.types.ObjectId id, String status) {
        col.updateOne(
                Filters.eq("_id", id),
                com.mongodb.client.model.Updates.set("status", status)
        );
    }

    /** Marks a subscription verified and stores the generated bill location. */
    public void verifyAndStoreBill(org.bson.types.ObjectId id, LocalDateTime verifiedDate, String billPath) {
        col.updateOne(
                Filters.eq("_id", id),
                com.mongodb.client.model.Updates.combine(
                        com.mongodb.client.model.Updates.set("status", "ACTIVE"),
                        com.mongodb.client.model.Updates.set("verifiedDate", toDate(verifiedDate)),
                        com.mongodb.client.model.Updates.set("billPath", billPath)
                )
        );
    }

    /** Updates subscription plan. */
    public void updatePlan(org.bson.types.ObjectId id, String plan) {
        col.updateOne(
                Filters.eq("_id", id),
                com.mongodb.client.model.Updates.set("plan", plan)
        );
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private Subscription toSub(Document d) {
        Subscription s = new Subscription();
        s.setId(d.getObjectId("_id"));
        s.setUsername(d.getString("username"));
        s.setPlan(d.getString("plan"));
        s.setStatus(d.getString("status"));
        s.setStartDate(toLocal(d.getDate("startDate")));
        Date end = d.getDate("endDate");
        s.setEndDate(end != null ? toLocal(end) : null);
        s.setPaymentMethod(d.getString("paymentMethod"));
        s.setPaymentDetail(d.getString("paymentDetail"));
        Date verified = d.getDate("verifiedDate");
        s.setVerifiedDate(verified != null ? toLocal(verified) : null);
        s.setBillPath(d.getString("billPath"));
        return s;
    }

    private Date toDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocal(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
