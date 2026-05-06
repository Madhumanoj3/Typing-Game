package service;

import db.AchievementDAO;
import db.TrainingCertificateDAO;
import model.Achievement;
import model.Subscription;
import model.TrainingCertificate;
import model.TrainingProgress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Premium-only typing certificate generation after all lessons are complete.
 */
public class CertificationService {

    private static CertificationService instance;

    private final TrainingService trainingService = TrainingService.getInstance();
    private final PaymentService paymentService = PaymentService.getInstance();
    private final TrainingCertificateDAO certificateDAO = TrainingCertificateDAO.getInstance();

    private CertificationService() {}

    public static CertificationService getInstance() {
        if (instance == null) instance = new CertificationService();
        return instance;
    }

    public boolean isEligible(String username) {
        return trainingService.isPremiumUser(username)
                && trainingService.totalLessons() > 0
                && trainingService.countCompleted(username) >= trainingService.totalLessons();
    }

    public boolean canExport(String username) {
        TrainingCertificate cert = certificateDAO.getForUser(username);
        return isEligible(username) && cert != null && "APPROVED".equals(cert.getStatus());
    }

    public TrainingCertificate getCertificate(String username) {
        return certificateDAO.getForUser(username);
    }

    public String statusMessage(String username) {
        int completed = trainingService.countCompleted(username);
        int total = trainingService.totalLessons();
        if (!trainingService.isPremiumUser(username)) return "Premium required for certificates";
        if (total == 0) return "No lessons available yet";
        if (completed < total) return (total - completed) + " lesson" + (total - completed == 1 ? "" : "s") + " remaining";
        TrainingCertificate cert = certificateDAO.getForUser(username);
        if (cert == null) return "Ready for admin review";
        if ("APPROVED".equals(cert.getStatus())) return "Grade " + cert.getGrade() + " approved";
        return "Sent to admin for grading";
    }

    public TrainingCertificate requestReviewIfEligible(String username) {
        if (!isEligible(username)) return certificateDAO.getForUser(username);
        TrainingCertificate existing = certificateDAO.getForUser(username);
        if (existing != null) return existing;

        List<TrainingProgress> progress = trainingService.getProgress(username);
        TrainingCertificate cert = new TrainingCertificate();
        cert.setUsername(username);
        cert.setStatus("PENDING");
        cert.setCompletedLessons(trainingService.countCompleted(username));
        cert.setTotalLessons(trainingService.totalLessons());
        cert.setAverageWpm(progress.stream().mapToDouble(TrainingProgress::getBestWpm).average().orElse(0));
        cert.setAverageAccuracy(progress.stream().mapToDouble(TrainingProgress::getBestAccuracy).average().orElse(0));
        cert.setRequestedAt(LocalDate.now().atStartOfDay());
        certificateDAO.upsertRequest(cert);
        LivePerformanceService.getInstance().publishNotification(username, "Training Complete", "Certificate request sent to admin for grading.");
        return cert;
    }

    public Path exportCertificate(String username) throws IOException {
        if (!canExport(username)) {
            throw new IllegalStateException(statusMessage(username));
        }

        List<TrainingProgress> progress = trainingService.getProgress(username);
        TrainingCertificate cert = certificateDAO.getForUser(username);
        double bestWpm = progress.stream().mapToDouble(TrainingProgress::getBestWpm).max().orElse(0);
        double bestAccuracy = progress.stream().mapToDouble(TrainingProgress::getBestAccuracy).average().orElse(0);
        Subscription subscription = paymentService.getSubscription(username);
        String certId = "TYPEMASTER-" + Math.abs((username + LocalDate.now()).hashCode());

        Path dir = Path.of("certificates");
        Files.createDirectories(dir);
        Path out = dir.resolve(certId + ".html");

        String issued = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String validUntil = subscription != null && subscription.getEndDate() != null
                ? subscription.getEndDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Premium active";

        String html = """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <title>TypeMaster Certificate</title>
                  <style>
                    body { margin: 0; font-family: Segoe UI, Arial, sans-serif; background: #0f0f1a; color: #e2e8f0; }
                    .cert { margin: 48px auto; width: 900px; min-height: 560px; padding: 56px; box-sizing: border-box;
                            background: #12122b; border: 2px solid #8b5cf6; border-radius: 18px; }
                    h1 { margin: 0; color: #c4b5fd; font-size: 38px; letter-spacing: 0; }
                    h2 { margin: 42px 0 8px; color: white; font-size: 34px; }
                    .muted { color: #94a3b8; font-size: 16px; }
                    .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-top: 34px; }
                    .box { background: #0f172a; border-radius: 12px; padding: 18px; }
                    .num { color: #38bdf8; font-weight: 700; font-size: 28px; }
                    .footer { margin-top: 44px; display: flex; justify-content: space-between; color: #94a3b8; }
                  </style>
                </head>
                <body>
                  <main class="cert">
                    <h1>TypeMaster Skill Certificate</h1>
                    <p class="muted">This verifies successful completion of the full typing training path.</p>
                    <h2>%s</h2>
                    <p class="muted">Premium learner certification with skill verification badge.</p>
                    <section class="grid">
                      <div class="box"><div class="num">%d/%d</div><div>Lessons completed</div></div>
                      <div class="box"><div class="num">%.0f</div><div>Best WPM</div></div>
                      <div class="box"><div class="num">%.1f%%</div><div>Average accuracy</div></div>
                      <div class="box"><div class="num">%s</div><div>Admin grade</div></div>
                      <div class="box"><div class="num">%.0f</div><div>Average WPM</div></div>
                      <div class="box"><div class="num">%s</div><div>Verified by</div></div>
                    </section>
                    <div class="footer">
                      <span>Certificate ID: %s</span>
                      <span>Issued: %s</span>
                      <span>Premium: %s</span>
                    </div>
                  </main>
                </body>
                </html>
                """.formatted(escapeHtml(username), trainingService.countCompleted(username), trainingService.totalLessons(),
                bestWpm, bestAccuracy, escapeHtml(cert.getGrade()), cert.getAverageWpm(),
                escapeHtml(cert.getGradedBy() != null ? cert.getGradedBy() : "Admin"), certId, issued, validUntil);

        Files.writeString(out, html, StandardCharsets.UTF_8);
        certificateDAO.saveCertificatePath(username, out.toString());
        grantCertificateBadge(username);
        LivePerformanceService.getInstance().publishNotification(username, "Certificate Ready", "Typing certificate exported successfully.");
        return out;
    }

    private void grantCertificateBadge(String username) {
        AchievementDAO dao = AchievementDAO.getInstance();
        if (!dao.hasAchievement(username, "CERTIFIED_TYPIST")) {
            dao.save(new Achievement(username, "CERTIFIED_TYPIST", "Certified Typist",
                    "Completed all lessons and exported a premium typing certificate"));
        }
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
