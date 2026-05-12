package com.hireconnect.notification.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.hireconnect.notification.client.JobSnapshot;
import com.hireconnect.notification.client.ProfileSnapshot;
import com.lowagie.text.Chunk;
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
public class OfferLetterPdfService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Color NAVY = new Color(20, 17, 63);
    private static final Color MAGENTA = new Color(204, 38, 153);
    private static final Color GREEN = new Color(128, 238, 141);
    private static final Color TEXT = new Color(54, 58, 76);

    public OfferLetterAttachment build(ProfileSnapshot candidate, ProfileSnapshot recruiter, JobSnapshot job) {
        int annualSalary = randomAnnualSalary(job.salaryMin(), job.salaryMax());
        int monthlySalary = Math.max(1, Math.round(annualSalary / 12.0f));
        String companyName = companyName(job, recruiter);
        String candidateName = displayName(candidate);
        String roleTitle = StringUtils.hasText(job.title()) ? job.title() : "the offered role";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 56, 56, 0, 48);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, companyName);
            addAddressBlock(document, companyName, candidate);
            addTitle(document);
            addBody(document, candidateName, companyName, roleTitle, job, annualSalary, monthlySalary);
            addSignature(document, recruiter);

            document.close();
            return new OfferLetterAttachment(filename(companyName, candidateName), out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate offer letter PDF", ex);
        }
    }

    private void addHeader(Document document, String companyName) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 1.4f, 1f });

        PdfPCell brand = new PdfPCell();
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setBackgroundColor(NAVY);
        brand.setPadding(26);
        brand.addElement(new Paragraph(companyName.toUpperCase(Locale.ROOT), font(20, Font.BOLD, Color.WHITE)));
        brand.addElement(new Paragraph("HireConnect verified offer", font(10, Font.NORMAL, new Color(214, 222, 245))));

        PdfPCell date = new PdfPCell(new Phrase("Date: " + DATE_FORMAT.format(LocalDate.now()), font(12, Font.BOLD, Color.WHITE)));
        date.setHorizontalAlignment(Element.ALIGN_RIGHT);
        date.setVerticalAlignment(Element.ALIGN_MIDDLE);
        date.setBorder(Rectangle.NO_BORDER);
        date.setBackgroundColor(MAGENTA);
        date.setPadding(26);

        header.addCell(brand);
        header.addCell(date);
        document.add(header);
        document.add(Chunk.NEWLINE);
    }

    private void addAddressBlock(Document document, String companyName, ProfileSnapshot candidate) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1f, 1f });
        table.setSpacingBefore(22);
        table.setSpacingAfter(22);

        table.addCell(addressCell(companyName, "Hiring team\nHireConnect partner company", Element.ALIGN_LEFT));
        table.addCell(addressCell(displayName(candidate), safe(candidate.email()) + "\nCandidate", Element.ALIGN_RIGHT));
        document.add(table);
    }

    private PdfPCell addressCell(String heading, String details, int align) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.addElement(aligned(new Paragraph(heading, font(13, Font.BOLD, MAGENTA)), align));
        cell.addElement(aligned(new Paragraph(details, font(10, Font.NORMAL, TEXT)), align));
        return cell;
    }

    private void addTitle(Document document) throws Exception {
        Paragraph title = new Paragraph("EMPLOYMENT\nOFFER LETTER", font(28, Font.BOLD, GREEN));
        title.setLeading(34);
        title.setSpacingAfter(30);
        document.add(title);
    }

    private void addBody(
        Document document,
        String candidateName,
        String companyName,
        String roleTitle,
        JobSnapshot job,
        int annualSalary,
        int monthlySalary
    ) throws Exception {
        document.add(paragraph("Dear " + candidateName + ","));
        document.add(paragraph("We are delighted to inform you that, based on your skills and interview performance, you have been selected for the position of " + roleTitle + " at " + companyName + "."));
        document.add(paragraph("This is a full-time offer for the " + roleTitle + " role. Your work location is " + safe(job.location(), "as discussed with the recruiter") + ", and your employment type is " + safe(job.type(), "FULL_TIME") + "."));
        document.add(paragraph("Your offered annual salary is " + money(annualSalary) + ". This equals a monthly salary of " + money(monthlySalary) + " before applicable taxes and statutory deductions."));
        document.add(paragraph("You may be asked to complete standard onboarding formalities, document verification, company policy acceptance, and any required confidentiality or employment agreements before joining."));
        document.add(paragraph("Please respond to this offer from your HireConnect candidate workspace. If we do not receive a response within the stated hiring timeline, the company may move forward with the next suitable candidate."));
        document.add(paragraph("We look forward to welcoming you to " + companyName + "."));
    }

    private void addSignature(Document document, ProfileSnapshot recruiter) throws Exception {
        Paragraph regards = paragraph("Best Regards,");
        regards.setSpacingBefore(18);
        document.add(regards);
        document.add(new Paragraph(displayName(recruiter), font(12, Font.BOLD, MAGENTA)));
        document.add(new Paragraph("Hiring Manager", font(10, Font.NORMAL, TEXT)));
        Paragraph line = new Paragraph("________________________", font(18, Font.NORMAL, new Color(170, 230, 174)));
        line.setSpacingBefore(28);
        document.add(line);
        document.add(new Paragraph("Signature", font(9, Font.NORMAL, TEXT)));
    }

    private Paragraph paragraph(String text) {
        Paragraph paragraph = new Paragraph(text, font(11, Font.NORMAL, TEXT));
        paragraph.setLeading(17);
        paragraph.setSpacingAfter(14);
        return paragraph;
    }

    private Element aligned(Paragraph paragraph, int align) {
        paragraph.setAlignment(align);
        return paragraph;
    }

    private Font font(int size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private int randomAnnualSalary(Integer min, Integer max) {
        int lower = min == null || min <= 0 ? 300000 : min;
        int upper = max == null || max < lower ? lower + 300000 : max;
        int raw = lower + RANDOM.nextInt((upper - lower) + 1);
        return Math.round(raw / 1000.0f) * 1000;
    }

    private String companyName(JobSnapshot job, ProfileSnapshot recruiter) {
        if (StringUtils.hasText(job.companyName())) {
            return job.companyName();
        }
        if (StringUtils.hasText(recruiter.fullName())) {
            return recruiter.fullName();
        }
        return "HireConnect Partner";
    }

    private String displayName(ProfileSnapshot profile) {
        if (profile == null) {
            return "Candidate";
        }
        return StringUtils.hasText(profile.fullName()) ? profile.fullName() : safe(profile.email(), "Candidate");
    }

    private String money(int amount) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(amount);
    }

    private String filename(String companyName, String candidateName) {
        return (companyName + "-offer-letter-" + candidateName)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "")
            + ".pdf";
    }

    private String safe(String value) {
        return safe(value, "");
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
