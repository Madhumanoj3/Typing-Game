package service;

import db.AchievementDAO;
import db.TrainingCertificateDAO;
import model.Achievement;
import model.Subscription;
import model.TrainingCertificate;
import model.TrainingProgress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Premium-only typing certificate generation after all lessons are complete.
 * Exports a professionally designed PDF certificate (A4 landscape).
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
        LivePerformanceService.getInstance().publishNotification(username, "Training Complete",
                "Certificate request sent to admin for grading.");
        return cert;
    }

    /** Export an approved certificate as a well-designed PDF (A4 landscape, 842x595 pt). */
    public Path exportCertificate(String username) throws IOException {
        if (!canExport(username)) {
            throw new IllegalStateException(statusMessage(username));
        }

        List<TrainingProgress> progress = trainingService.getProgress(username);
        TrainingCertificate cert = certificateDAO.getForUser(username);
        double bestWpm     = progress.stream().mapToDouble(TrainingProgress::getBestWpm).max().orElse(0);
        double avgAccuracy = progress.stream().mapToDouble(TrainingProgress::getBestAccuracy).average().orElse(0);
        Subscription subscription = paymentService.getSubscription(username);
        String certId = "TYPEMASTER-" + Math.abs((username + LocalDate.now()).hashCode());

        Path dir = Path.of("certificates");
        Files.createDirectories(dir);
        Path out = dir.resolve(certId + ".pdf");

        String issued = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String validUntil = subscription != null && subscription.getEndDate() != null
                ? subscription.getEndDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Lifetime";
        String gradedBy = cert.getGradedBy() != null ? cert.getGradedBy() : "TypeMaster Admin";
        String grade    = cert.getGrade()    != null ? cert.getGrade()    : "A";

        byte[] pdf = buildCertPdf(username, certId, issued, validUntil,
                trainingService.countCompleted(username), trainingService.totalLessons(),
                bestWpm, avgAccuracy, cert.getAverageWpm(), grade, gradedBy);

        Files.write(out, pdf);
        certificateDAO.saveCertificatePath(username, out.toString());
        grantCertificateBadge(username);
        LivePerformanceService.getInstance().publishNotification(username, "Certificate Ready",
                "Typing certificate exported as PDF successfully.");
        return out;
    }

    // ── PDF generation ────────────────────────────────────────────────────

    private byte[] buildCertPdf(String username, String certId, String issued, String validUntil,
            int completed, int total, double bestWpm, double avgAccuracy,
            double avgWpm, String grade, String gradedBy) throws IOException {

        // A4 landscape: 842 x 595 pt
        final int W = 842, H = 595;
        StringBuilder c = new StringBuilder();

        // ── Background: cream/ivory ───────────────────────────────────────
        c.append("0.99 0.98 0.94 rg\n");
        c.append("0 0 842 595 re\nf\n");

        // ── Outer decorative border (double) ─────────────────────────────
        c.append("0.43 0.20 0.60 G\n2.0 w\n");
        c.append("18 18 806 559 re\nS\n");
        c.append("0.63 0.40 0.80 G\n0.8 w\n");
        c.append("26 26 790 543 re\nS\n");

        // ── Top header band ───────────────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        c.append("26 520 790 50 re\nf\n");

        // TypeMaster name (white)
        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 26 Tf\n280 546 Td\n(TypeMaster) Tj\nET\n");

        // Subtitle row
        c.append("0.80 0.70 1.0 rg\n");
        c.append("BT\n/F1 11 Tf\n240 530 Td\n(Premium Typing Training Programme) Tj\nET\n");

        // ── Certificate title ─────────────────────────────────────────────
        c.append("0.43 0.20 0.60 rg\n");
        c.append("BT\n/F2 22 Tf\n230 495 Td\n(CERTIFICATE  OF  ACHIEVEMENT) Tj\nET\n");

        // Decorative underline below title
        c.append("0.63 0.40 0.80 G\n1.2 w\n");
        c.append("150 488 m\n692 488 l\nS\n");
        c.append("0.43 0.20 0.60 G\n0.5 w\n");
        c.append("200 484 m\n642 484 l\nS\n");

        // ── Award text ────────────────────────────────────────────────────
        c.append("0.30 0.30 0.30 rg\n");
        c.append("BT\n/F1 11 Tf\n310 462 Td\n(This certifies that) Tj\nET\n");

        // Recipient name (large, purple)
        c.append("0.28 0.10 0.55 rg\n");
        c.append("BT\n/F2 30 Tf\n" + centerX(username, 30, W) + " 432 Td\n(" + e(username) + ") Tj\nET\n");

        // Decorative line under name
        c.append("0.63 0.40 0.80 G\n0.8 w\n");
        int nameLineX = Math.max(150, 421 - username.length() * 9);
        int nameLineW = Math.min(540, username.length() * 18 + 80);
        c.append(nameLineX + " 424 m\n" + (nameLineX + nameLineW) + " 424 l\nS\n");

        c.append("0.30 0.30 0.30 rg\n");
        c.append("BT\n/F1 11 Tf\n250 408 Td\n(has successfully completed the TypeMaster Premium Typing Training Program) Tj\nET\n");
        c.append("BT\n/F1 10 Tf\n305 393 Td\n(with dedication, skill, and exceptional performance.) Tj\nET\n");

        // ── Stats table ───────────────────────────────────────────────────
        int tableX = 80, tableY = 340, colW = 130, rowH = 36;

        // Table header
        c.append("0.18 0.07 0.37 rg\n");
        for (int i = 0; i < 5; i++)
            c.append((tableX + i * colW) + " " + tableY + " " + colW + " " + rowH + " re\nf\n");
        c.append("1 1 1 rg\n");
        statHeader(c, tableX,           tableY, colW, "Lessons");
        statHeader(c, tableX + colW,    tableY, colW, "Best WPM");
        statHeader(c, tableX + 2*colW,  tableY, colW, "Avg Accuracy");
        statHeader(c, tableX + 3*colW,  tableY, colW, "Avg WPM");
        statHeader(c, tableX + 4*colW,  tableY, colW, "Grade");

        // Table values row
        c.append("0.97 0.95 1.0 rg\n");
        for (int i = 0; i < 5; i++)
            c.append((tableX + i * colW) + " " + (tableY - rowH) + " " + colW + " " + rowH + " re\nf\n");

        c.append("0.18 0.07 0.37 rg\n");
        statValue(c, tableX,           tableY - rowH, colW, completed + "/" + total);
        statValue(c, tableX + colW,    tableY - rowH, colW, String.format("%.0f", bestWpm));
        statValue(c, tableX + 2*colW,  tableY - rowH, colW, String.format("%.1f%%", avgAccuracy));
        statValue(c, tableX + 3*colW,  tableY - rowH, colW, String.format("%.0f", avgWpm));
        statValue(c, tableX + 4*colW,  tableY - rowH, colW, grade);

        // Table border
        c.append("0.55 0.30 0.75 G\n0.8 w\n");
        c.append(tableX + " " + (tableY - rowH) + " " + (5*colW) + " " + (2*rowH) + " re\nS\n");
        for (int i = 1; i < 5; i++) {
            int x = tableX + i * colW;
            c.append(x + " " + tableY + " m\n" + x + " " + (tableY - rowH) + " l\nS\n");
        }
        c.append(tableX + " " + tableY + " m\n" + (tableX + 5*colW) + " " + tableY + " l\nS\n");

        // ── Verified by + signature area ──────────────────────────────────
        c.append("0.43 0.43 0.43 rg\n");
        c.append("BT\n/F1 9 Tf\n80 258 Td\n(Verified and approved by:) Tj\nET\n");
        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 10 Tf\n80 245 Td\n(" + e(gradedBy) + ") Tj\nET\n");
        c.append("0.55 0.30 0.75 G\n0.5 w\n");
        c.append("80 240 m\n220 240 l\nS\n");

        // ── Seal circle ───────────────────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        appendCircle(c, 740, 268, 45);
        c.append("f\n");
        c.append("0.63 0.40 0.80 G\n1.5 w\n");
        appendCircle(c, 740, 268, 45);
        c.append("S\n");
        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 8 Tf\n719 276 Td\n(VERIFIED) Tj\nET\n");
        c.append("BT\n/F1 10 Tf\n722 263 Td\n(Grade " + e(grade) + ") Tj\nET\n");
        c.append("BT\n/F1 7 Tf\n717 250 Td\n(TypeMaster) Tj\nET\n");

        // ── Bottom footer band ────────────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        c.append("26 26 790 32 re\nf\n");

        c.append("0.75 0.65 0.95 rg\n");
        c.append("BT\n/F1 8 Tf\n40 37 Td\n(Certificate ID: " + e(certId) + ") Tj\nET\n");
        c.append("BT\n/F1 8 Tf\n310 37 Td\n(Issued: " + e(issued) + ") Tj\nET\n");
        c.append("BT\n/F1 8 Tf\n540 37 Td\n(Premium Valid Until: " + e(validUntil) + ") Tj\nET\n");

        // ── Assemble PDF (A4 landscape) ───────────────────────────────────
        List<byte[]> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] " +
                "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>")
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"
                .getBytes(StandardCharsets.ISO_8859_1));

        byte[] stream = c.toString().getBytes(StandardCharsets.ISO_8859_1);
        objects.add(("<< /Length " + stream.length + " >>\nstream\n" + c + "endstream")
                .getBytes(StandardCharsets.ISO_8859_1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int offset : offsets)
            out.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n" +
                "startxref\n" + xref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    // ── Drawing helpers ───────────────────────────────────────────────────

    private void statHeader(StringBuilder c, int x, int y, int w, String label) {
        int textX = x + w / 2 - label.length() * 3;
        c.append("BT\n/F1 9 Tf\n" + textX + " " + (y + 13) + " Td\n(" + e(label) + ") Tj\nET\n");
    }

    private void statValue(StringBuilder c, int x, int y, int w, String val) {
        int textX = x + w / 2 - val.length() * 5;
        c.append("BT\n/F2 13 Tf\n" + textX + " " + (y + 11) + " Td\n(" + e(val) + ") Tj\nET\n");
    }

    /** Approximate center-X for a text string at the given font size on an 842-wide page. */
    private int centerX(String text, int fontSize, int pageW) {
        int approxWidth = text.length() * fontSize / 2;
        return Math.max(40, (pageW - approxWidth) / 2);
    }

    /** Append a circle path (Bézier approximation) centred at (cx, cy) with radius r. */
    private void appendCircle(StringBuilder c, double cx, double cy, double r) {
        double k = 0.5523;
        double kr = k * r;
        c.append(String.format("%.1f %.1f m\n", cx + r, cy));
        c.append(String.format("%.1f %.1f %.1f %.1f %.1f %.1f c\n",
                cx+r, cy+kr, cx+kr, cy+r, cx, cy+r));
        c.append(String.format("%.1f %.1f %.1f %.1f %.1f %.1f c\n",
                cx-kr, cy+r, cx-r, cy+kr, cx-r, cy));
        c.append(String.format("%.1f %.1f %.1f %.1f %.1f %.1f c\n",
                cx-r, cy-kr, cx-kr, cy-r, cx, cy-r));
        c.append(String.format("%.1f %.1f %.1f %.1f %.1f %.1f c\n",
                cx+kr, cy-r, cx+r, cy-kr, cx+r, cy));
        c.append("h\n");
    }

    /** Escape special PDF string characters (ISO-8859-1 safe). */
    private String e(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\n", " ");
    }

    // ── Badge & notification helpers ──────────────────────────────────────

    private void grantCertificateBadge(String username) {
        AchievementDAO dao = AchievementDAO.getInstance();
        if (!dao.hasAchievement(username, "CERTIFIED_TYPIST")) {
            dao.save(new Achievement(username, "CERTIFIED_TYPIST", "Certified Typist",
                    "Completed all lessons and exported a premium typing certificate"));
        }
    }
}
