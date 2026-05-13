package service;

import model.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained PDF invoice writer for verified premium subscriptions.
 */
public class BillPdfService {

    private static BillPdfService instance;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static BillPdfService getInstance() {
        if (instance == null) instance = new BillPdfService();
        return instance;
    }

    public Path generateBill(Subscription sub) throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"), "bills");
        Files.createDirectories(dir);
        String billNo = getBillNumber(sub);
        Path file = dir.resolve(billNo + ".pdf");
        Files.write(file, buildPdf(sub, billNo));
        return file;
    }

    public Path copyBillToDownloads(Subscription sub) throws IOException {
        Path source = resolveExistingBill(sub);
        if (source == null || !Files.exists(source)) {
            source = generateBill(sub);
        }
        Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
        if (!Files.isDirectory(downloads)) downloads = Path.of(System.getProperty("user.home"));
        Path target = downloads.resolve(source.getFileName());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private Path resolveExistingBill(Subscription sub) {
        if (sub.getBillPath() == null || sub.getBillPath().isBlank()) return null;
        return Path.of(sub.getBillPath());
    }

    // ── PDF construction ──────────────────────────────────────────────────

    private byte[] buildPdf(Subscription sub, String billNo) throws IOException {
        StringBuilder c = new StringBuilder();   // content stream

        String plan   = "LIFETIME".equals(sub.getPlan()) ? "LIFETIME" : "MONTHLY";
        String amount = "LIFETIME".equals(sub.getPlan()) ? "Rs. 1,999 / year" : "Rs. 199 / month";
        String requested  = sub.getStartDate()    != null ? sub.getStartDate().format(DATE_FMT)    : "-";
        String verified   = sub.getVerifiedDate() != null ? sub.getVerifiedDate().format(DATE_FMT) : LocalDateTime.now().format(DATE_FMT);
        String validUntil = sub.getEndDate()      != null ? sub.getEndDate().format(DATE_ONLY)      : "No expiry (Lifetime)";
        String method     = sub.getPaymentMethod() != null ? sub.getPaymentMethod() : "CARD";
        String detail     = sub.getPaymentDetail() != null ? sub.getPaymentDetail() : "-";

        // ── Header band (dark purple) ─────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        c.append("0 718 612 74 re\nf\n");

        // Header text — white
        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 22 Tf\n40 768 Td\n(TypeMaster Premium) Tj\nET\n");
        c.append("BT\n/F1 12 Tf\n40 749 Td\n(Subscription Invoice - Training Access) Tj\nET\n");

        // Bill No + date (right side of header)
        c.append("BT\n/F1 9 Tf\n380 768 Td\n(Invoice No: " + e(billNo) + ") Tj\nET\n");
        String nowStr = LocalDateTime.now().format(DATE_FMT);
        c.append("BT\n/F1 9 Tf\n380 754 Td\n(Issued On: " + e(nowStr) + ") Tj\nET\n");

        // ── Outer page border ─────────────────────────────────────────────
        c.append("0.55 0.25 0.80 G\n1.5 w\n");
        c.append("20 20 572 752 re\nS\n");

        // ── CUSTOMER DETAILS section ──────────────────────────────────────
        c.append("0.93 0.90 0.98 rg\n");
        c.append("40 660 532 34 re\nf\n");
        c.append("0.55 0.25 0.80 G\n0.5 w\n");
        c.append("40 660 532 34 re\nS\n");

        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 10 Tf\n48 674 Td\n(CUSTOMER DETAILS) Tj\nET\n");

        c.append("0 0 0 rg\n");
        c.append("BT\n/F1 10 Tf\n48 650 Td\n(Customer Name:) Tj\nET\n");
        c.append("BT\n/F1 10 Tf\n160 650 Td\n(" + e(sub.getUsername()) + ") Tj\nET\n");

        c.append("BT\n/F1 10 Tf\n48 635 Td\n(Subscription Plan:) Tj\nET\n");
        c.append("BT\n/F1 10 Tf\n160 635 Td\n(" + e(plan + "  —  " + amount) + ") Tj\nET\n");

        // Separator
        c.append("0.75 0.75 0.75 G\n0.4 w\n");
        c.append("40 622 m\n572 622 l\nS\n");

        // ── PAYMENT INFORMATION section ───────────────────────────────────
        c.append("0.93 0.90 0.98 rg\n");
        c.append("40 588 532 34 re\nf\n");
        c.append("0.55 0.25 0.80 G\n0.5 w\n");
        c.append("40 588 532 34 re\nS\n");

        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 10 Tf\n48 602 Td\n(PAYMENT INFORMATION) Tj\nET\n");

        c.append("0 0 0 rg\n");
        row(c, 578, "Payment Method:", method);
        row(c, 563, "Payment Detail:",  detail);
        row(c, 548, "Amount Paid:",     amount);
        row(c, 533, "Requested On:",    requested);
        row(c, 518, "Verified On:",     verified);

        // Separator
        c.append("0.75 0.75 0.75 G\n0.4 w\n");
        c.append("40 505 m\n572 505 l\nS\n");

        // ── TRAINING ACCESS section ───────────────────────────────────────
        c.append("0.93 0.90 0.98 rg\n");
        c.append("40 471 532 34 re\nf\n");
        c.append("0.55 0.25 0.80 G\n0.5 w\n");
        c.append("40 471 532 34 re\nS\n");

        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 10 Tf\n48 485 Td\n(TRAINING ACCESS) Tj\nET\n");

        c.append("0 0 0 rg\n");
        row(c, 461, "Premium Valid Until:", validUntil);
        row(c, 446, "Unlocks:",             "All 10 lessons + Advanced drills + Analytics");
        row(c, 431, "Status:",              "VERIFIED & ACTIVE");

        // Status badge (green box)
        c.append("0.06 0.48 0.32 rg\n");
        c.append("200 424 200 16 re\nf\n");
        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 10 Tf\n215 428 Td\n(Payment verified and premium access activated) Tj\nET\n");

        // Separator
        c.append("0.75 0.75 0.75 G\n0.4 w\n");
        c.append("40 412 m\n572 412 l\nS\n");

        // ── Amount summary box ────────────────────────────────────────────
        c.append("0.96 0.94 1.0 rg\n");
        c.append("340 358 230 50 re\nf\n");
        c.append("0.55 0.25 0.80 G\n0.8 w\n");
        c.append("340 358 230 50 re\nS\n");

        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 11 Tf\n356 395 Td\n(Total Amount Paid) Tj\nET\n");
        c.append("BT\n/F1 18 Tf\n356 370 Td\n(" + e(amount) + ") Tj\nET\n");

        // ── Footer ────────────────────────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        c.append("0 25 612 50 re\nf\n");

        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 10 Tf\n40 58 Td\n(Thank you for subscribing to TypeMaster Premium!) Tj\nET\n");
        c.append("BT\n/F1 9 Tf\n40 43 Td\n(This receipt confirms your training access. For support, contact the admin.) Tj\nET\n");

        // Right side of footer - invoice ref
        c.append("BT\n/F1 8 Tf\n380 58 Td\n(" + e(billNo) + ") Tj\nET\n");

        // ── Assemble PDF objects ──────────────────────────────────────────
        List<byte[]> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>")
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

    /** Render a two-column label/value row at the given y position. */
    private void row(StringBuilder c, int y, String label, String value) {
        c.append("0.35 0.35 0.35 rg\n");
        c.append("BT\n/F1 9 Tf\n48 " + y + " Td\n(" + e(label) + ") Tj\nET\n");
        c.append("0 0 0 rg\n");
        c.append("BT\n/F1 9 Tf\n175 " + y + " Td\n(" + e(value) + ") Tj\nET\n");
    }

    public String getBillNumber(Subscription sub) {
        String idPart = sub.getId() != null ? sub.getId().toHexString().substring(0, 8).toUpperCase() : "LOCAL";
        return "PREMIUM-BILL-" + idPart;
    }

    /** Escape special PDF string characters. */
    private String e(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("\n", " ");
    }
}
