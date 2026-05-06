package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import model.TrainingCertificate;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrainingCertificateDAO {

    private static TrainingCertificateDAO instance;
    private final MongoCollection<Document> col;

    private TrainingCertificateDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("training_certificates");
        col.createIndex(new Document("username", 1), new IndexOptions().unique(true));
    }

    public static TrainingCertificateDAO getInstance() {
        if (instance == null) instance = new TrainingCertificateDAO();
        return instance;
    }

    public void upsertRequest(TrainingCertificate cert) {
        col.replaceOne(Filters.eq("username", cert.getUsername()), toDoc(cert),
                new ReplaceOptions().upsert(true));
    }

    public TrainingCertificate getForUser(String username) {
        Document doc = col.find(Filters.eq("username", username)).first();
        return doc == null ? null : fromDoc(doc);
    }

    public List<TrainingCertificate> getAll() {
        List<TrainingCertificate> list = new ArrayList<>();
        for (Document doc : col.find().sort(Sorts.descending("requestedAt"))) {
            list.add(fromDoc(doc));
        }
        return list;
    }

    public long countPending() {
        return col.countDocuments(Filters.eq("status", "PENDING"));
    }

    public void approveWithGrade(String username, String grade, String gradedBy) {
        col.updateOne(Filters.eq("username", username),
                Updates.combine(
                        Updates.set("status", "APPROVED"),
                        Updates.set("grade", grade),
                        Updates.set("gradedBy", gradedBy),
                        Updates.set("gradedAt", toDate(LocalDateTime.now()))
                ));
    }

    public void saveCertificatePath(String username, String path) {
        col.updateOne(Filters.eq("username", username), Updates.set("certificatePath", path));
    }

    private Document toDoc(TrainingCertificate c) {
        return new Document()
                .append("username", c.getUsername())
                .append("status", c.getStatus())
                .append("grade", c.getGrade())
                .append("averageWpm", c.getAverageWpm())
                .append("averageAccuracy", c.getAverageAccuracy())
                .append("completedLessons", c.getCompletedLessons())
                .append("totalLessons", c.getTotalLessons())
                .append("requestedAt", toDate(c.getRequestedAt()))
                .append("gradedAt", toDate(c.getGradedAt()))
                .append("gradedBy", c.getGradedBy())
                .append("certificatePath", c.getCertificatePath());
    }

    private TrainingCertificate fromDoc(Document d) {
        TrainingCertificate c = new TrainingCertificate();
        c.setId(d.getObjectId("_id"));
        c.setUsername(d.getString("username"));
        c.setStatus(d.getString("status"));
        c.setGrade(d.getString("grade"));
        c.setAverageWpm(numberAsDouble(d, "averageWpm"));
        c.setAverageAccuracy(numberAsDouble(d, "averageAccuracy"));
        c.setCompletedLessons(d.getInteger("completedLessons", 0));
        c.setTotalLessons(d.getInteger("totalLessons", 0));
        c.setRequestedAt(toLocal(d.getDate("requestedAt")));
        c.setGradedAt(toLocal(d.getDate("gradedAt")));
        c.setGradedBy(d.getString("gradedBy"));
        c.setCertificatePath(d.getString("certificatePath"));
        return c;
    }

    private Date toDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocal(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private double numberAsDouble(Document doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }
}
