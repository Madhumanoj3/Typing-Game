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
 * Small self-contained PDF bill writer for verified premium subscriptions.
 */
public class BillPdfService {

    private static BillPdfService instance;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

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
        if (!Files.isDirectory(downloads)) {
            downloads = Path.of(System.getProperty("user.home"));
        }
        Path target = downloads.resolve(source.getFileName());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private Path resolveExistingBill(Subscription sub) {
        if (sub.getBillPath() == null || sub.getBillPath().isBlank()) return null;
        return Path.of(sub.getBillPath());
    }

    private byte[] buildPdf(Subscription sub, String billNo) throws IOException {
        List<String> lines = billLines(sub, billNo);
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 24 Tf\n50 760 Td\n(Typing Game Premium Bill) Tj\n");
        content.append("/F1 11 Tf\n0 -32 Td\n");
        for (String line : lines) {
            content.append("(").append(escape(line)).append(") Tj\n0 -20 Td\n");
        }
        content.append("ET\n");

        List<byte[]> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>")
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1));
        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        objects.add(("<< /Length " + stream.length + " >>\nstream\n" +
                content + "endstream").getBytes(StandardCharsets.ISO_8859_1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write((i + 1 + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int offset : offsets) {
            out.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
        }
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n" +
                "startxref\n" + xref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private List<String> billLines(Subscription sub, String billNo) {
        String amount = "LIFETIME".equals(sub.getPlan()) ? "Rs. 1,999 / year" : "Rs. 199 / month";
        String requested = sub.getStartDate() != null ? sub.getStartDate().format(DATE_FMT) : "-";
        LocalDateTime verifiedAt = sub.getVerifiedDate() != null ? sub.getVerifiedDate() : LocalDateTime.now();
        String validUntil = sub.getEndDate() != null ? sub.getEndDate().format(DATE_FMT) : "No expiry";
        String method = sub.getPaymentMethod() != null ? sub.getPaymentMethod() : "CARD";
        String detail = sub.getPaymentDetail() != null ? sub.getPaymentDetail() : "-";

        return List.of(
                "Bill No: " + billNo,
                "Customer: " + sub.getUsername(),
                "Plan: " + sub.getPlan(),
                "Amount Paid: " + amount,
                "Payment Method: " + method,
                "Payment Detail: " + detail,
                "Requested On: " + requested,
                "Verified On: " + verifiedAt.format(DATE_FMT),
                "Premium Valid Until: " + validUntil,
                "",
                "Status: Payment verified by admin and premium access activated.",
                "Thank you for upgrading to Typing Game Premium."
        );
    }

    public String getBillNumber(Subscription sub) {
        String idPart = sub.getId() != null ? sub.getId().toHexString().substring(0, 8).toUpperCase() : "LOCAL";
        return "PREMIUM-BILL-" + idPart;
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
