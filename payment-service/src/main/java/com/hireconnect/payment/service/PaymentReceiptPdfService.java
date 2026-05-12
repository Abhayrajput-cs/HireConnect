package com.hireconnect.payment.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.hireconnect.payment.domain.PaymentTransaction;
import com.hireconnect.payment.domain.SubscriptionPlan;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PaymentReceiptPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
        .withZone(ZoneId.of("Asia/Kolkata"));
    private static final Color NAVY = new Color(6, 23, 43);
    private static final Color BLUE = new Color(14, 165, 255);
    private static final Color TEXT = new Color(36, 54, 75);
    private static final Color MUTED = new Color(101, 117, 138);

    public PaymentReceiptAttachment build(PaymentTransaction transaction, SubscriptionPlan plan) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 42, 42);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document);
            addSummary(document, transaction, plan);
            addRows(document, transaction, plan);
            addFooter(document);

            document.close();
            return new PaymentReceiptAttachment(filename(transaction), out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate payment receipt PDF", ex);
        }
    }

    private void addHeader(Document document) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 1.3f, 1f });

        PdfPCell brand = new PdfPCell();
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setBackgroundColor(NAVY);
        brand.setPadding(24);
        brand.addElement(new Paragraph("HireConnect", font(24, Font.BOLD, Color.WHITE)));
        brand.addElement(new Paragraph("Subscription payment receipt", font(11, Font.NORMAL, new Color(210, 225, 244))));

        PdfPCell status = new PdfPCell(new Phrase("PAID", font(22, Font.BOLD, Color.WHITE)));
        status.setBorder(Rectangle.NO_BORDER);
        status.setBackgroundColor(BLUE);
        status.setPadding(24);
        status.setHorizontalAlignment(Element.ALIGN_RIGHT);
        status.setVerticalAlignment(Element.ALIGN_MIDDLE);

        header.addCell(brand);
        header.addCell(status);
        document.add(header);
    }

    private void addSummary(Document document, PaymentTransaction transaction, SubscriptionPlan plan) throws Exception {
        Paragraph title = new Paragraph("Receipt for " + plan.getDisplayName(), font(20, Font.BOLD, TEXT));
        title.setSpacingBefore(28);
        title.setSpacingAfter(8);
        document.add(title);

        document.add(paragraph("Issued to: " + safe(transaction.getCustomerName(), "Customer")));
        document.add(paragraph("Email: " + safe(transaction.getCustomerEmail(), "Not available")));
        document.add(paragraph("Amount paid: " + transaction.getCurrency() + " " + formatAmount(transaction.getAmount())));
    }

    private void addRows(Document document, PaymentTransaction transaction, SubscriptionPlan plan) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 0.8f, 1.2f });
        table.setSpacingBefore(20);
        table.setSpacingAfter(22);

        addRow(table, "Plan", plan.getDisplayName());
        addRow(table, "Role", transaction.getRole().name());
        addRow(table, "Order ID", transaction.getOrderId());
        addRow(table, "Payment ID", safe(transaction.getTransactionId(), "Not available"));
        addRow(table, "Status", transaction.getPaymentStatus().name());
        addRow(table, "Start date", formatDate(transaction.getStartDate()));
        addRow(table, "Expiry date", formatDate(transaction.getExpiryDate()));

        document.add(table);
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell left = cell(label, font(10, Font.BOLD, MUTED));
        PdfPCell right = cell(value, font(10, Font.BOLD, TEXT));
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private PdfPCell cell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(13);
        cell.setBorderColor(new Color(220, 231, 242));
        cell.setBackgroundColor(new Color(248, 251, 255));
        return cell;
    }

    private void addFooter(Document document) throws Exception {
        Paragraph note = paragraph("Keep this PDF as your billing receipt. For support, quote the Order ID shown above.");
        note.setSpacingBefore(16);
        document.add(note);
    }

    private Paragraph paragraph(String text) {
        Paragraph paragraph = new Paragraph(text, font(11, Font.NORMAL, TEXT));
        paragraph.setLeading(17);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private Font font(int size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String formatDate(Instant instant) {
        return instant == null ? "Not available" : DATE_FORMATTER.format(instant);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String filename(PaymentTransaction transaction) {
        String orderId = safe(transaction.getOrderId(), "receipt").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return "hireconnect-receipt-" + orderId + ".pdf";
    }
}
