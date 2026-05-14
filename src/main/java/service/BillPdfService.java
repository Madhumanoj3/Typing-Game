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
        // F1 = Helvetica-Bold (labels, headers), F2 = Helvetica (values, body text)

        String plan   = "LIFETIME".equals(sub.getPlan()) ? "LIFETIME" : "MONTHLY";
        String amount = "LIFETIME".equals(sub.getPlan()) ? "Rs. 1,999 / year" : "Rs. 199 / month";
        String planLine   = plan + " - " + amount;        // ASCII hyphen avoids ISO encoding issue
        String requested  = sub.getStartDate()    != null ? sub.getStartDate().format(DATE_FMT)    : "-";
        String verified   = sub.getVerifiedDate() != null ? sub.getVerifiedDate().format(DATE_FMT) : LocalDateTime.now().format(DATE_FMT);
        String validUntil = sub.getEndDate()      != null ? sub.getEndDate().format(DATE_ONLY)      : "No expiry (Lifetime)";
        String method     = sub.getPaymentMethod() != null ? sub.getPaymentMethod() : "CARD";
        String detail     = sub.getPaymentDetail() != null ? sub.getPaymentDetail() : "-";
        String nowStr     = LocalDateTime.now().format(DATE_FMT);

        // ── Outer page border ─────────────────────────────────────────────
        c.append("0.55 0.25 0.80 G\n1.5 w\n");
        c.append("18 18 576 756 re\nS\n");

        // ── Header band (dark purple) ─────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        c.append("18 706 576 86 re\nf\n");

        // Company name + subtitle
        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 22 Tf\n38 766 Td\n(TypeMaster Premium) Tj\nET\n");
        c.append("0.82 0.72 1.0 rg\n");
        c.append("BT\n/F2 11 Tf\n38 748 Td\n(Subscription Invoice - Training Access) Tj\nET\n");

        // Invoice No + date — right-aligned block
        c.append("0.82 0.72 1.0 rg\n");
        c.append("BT\n/F1 8 Tf\n390 766 Td\n(Invoice No:) Tj\nET\n");
        c.append("1 1 1 rg\n");
        c.append("BT\n/F2 8 Tf\n390 755 Td\n(" + e(billNo) + ") Tj\nET\n");
        c.append("0.82 0.72 1.0 rg\n");
        c.append("BT\n/F1 8 Tf\n390 742 Td\n(Issued On:) Tj\nET\n");
        c.append("1 1 1 rg\n");
        c.append("BT\n/F2 8 Tf\n390 731 Td\n(" + e(nowStr) + ") Tj\nET\n");

        // ── CUSTOMER DETAILS section ──────────────────────────────────────
        sectionHeader(c, 656, "CUSTOMER DETAILS");

        c.append("0 0 0 rg\n");
        row(c, 640, "Customer Name:",     sub.getUsername());
        row(c, 623, "Subscription Plan:", planLine);

        divider(c, 608);

        // ── PAYMENT INFORMATION section ───────────────────────────────────
        sectionHeader(c, 588, "PAYMENT INFORMATION");

        c.append("0 0 0 rg\n");
        row(c, 572, "Payment Method:", method);
        row(c, 555, "Payment Detail:",  detail);
        row(c, 538, "Amount Paid:",     amount);
        row(c, 521, "Requested On:",    requested);
        row(c, 504, "Verified On:",     verified);

        divider(c, 489);

        // ── TRAINING ACCESS section ───────────────────────────────────────
        sectionHeader(c, 469, "TRAINING ACCESS");

        c.append("0 0 0 rg\n");
        row(c, 453, "Premium Valid Until:", validUntil);
        row(c, 436, "Unlocks:",             "All 10 lessons + Advanced drills + Analytics");
        row(c, 419, "Status:",              "");

        // Status badge — full-width green pill starting at value column
        c.append("0.04 0.50 0.30 rg\n");
        c.append("175 411 356 18 re\nf\n");
        c.append("1 1 1 rg\n");
        c.append("BT\n/F2 9 Tf\n183 416 Td\n(Payment verified and premium access activated) Tj\nET\n");

        divider(c, 398);

        // ── Amount summary box ────────────────────────────────────────────
        c.append("0.96 0.93 1.0 rg\n");
        c.append("330 338 244 56 re\nf\n");
        c.append("0.55 0.25 0.80 G\n0.8 w\n");
        c.append("330 338 244 56 re\nS\n");

        c.append("0.30 0.10 0.55 rg\n");
        c.append("BT\n/F1 11 Tf\n346 379 Td\n(Total Amount Paid) Tj\nET\n");

        // Thin separator inside box
        c.append("0.75 0.60 0.90 G\n0.4 w\n");
        c.append("338 372 m\n566 372 l\nS\n");

        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 20 Tf\n346 348 Td\n(" + e(amount) + ") Tj\nET\n");

        // ── Footer ────────────────────────────────────────────────────────
        c.append("0.18 0.07 0.37 rg\n");
        c.append("18 18 576 40 re\nf\n");

        c.append("1 1 1 rg\n");
        c.append("BT\n/F1 9 Tf\n38 46 Td\n(Thank you for subscribing to TypeMaster Premium!) Tj\nET\n");
        c.append("0.82 0.72 1.0 rg\n");
        c.append("BT\n/F2 8 Tf\n38 33 Td\n(This receipt confirms your training access. For support, contact the admin.) Tj\nET\n");

        // Footer right — bill number
        c.append("0.82 0.72 1.0 rg\n");
        c.append("BT\n/F2 8 Tf\n390 40 Td\n(" + e(billNo) + ") Tj\nET\n");

        // ── Assemble PDF objects ──────────────────────────────────────────
        // Obj 1=Catalog, 2=Pages, 3=Page, 4=F1(Bold), 5=F2(Regular), 6=Content
        List<byte[]> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>")
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
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

    /** Purple section-header band with bold white label. */
    private void sectionHeader(StringBuilder c, int y, String title) {
        c.append("0.93 0.90 0.98 rg\n");
        c.append("38 " + y + " 536 22 re\nf\n");
        c.append("0.55 0.25 0.80 G\n0.5 w\n");
        c.append("38 " + y + " 536 22 re\nS\n");
        c.append("0.18 0.07 0.37 rg\n");
        c.append("BT\n/F1 9 Tf\n46 " + (y + 7) + " Td\n(" + e(title) + ") Tj\nET\n");
    }

    /** Thin horizontal divider. */
    private void divider(StringBuilder c, int y) {
        c.append("0.80 0.80 0.80 G\n0.4 w\n");
        c.append("38 " + y + " m\n574 " + y + " l\nS\n");
    }

    /** Two-column label (bold) / value (regular) row. */
    private void row(StringBuilder c, int y, String label, String value) {
        c.append("0.35 0.35 0.35 rg\n");
        c.append("BT\n/F1 9 Tf\n46 " + y + " Td\n(" + e(label) + ") Tj\nET\n");
        c.append("0.10 0.10 0.10 rg\n");
        c.append("BT\n/F2 9 Tf\n185 " + y + " Td\n(" + e(value) + ") Tj\nET\n");
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
