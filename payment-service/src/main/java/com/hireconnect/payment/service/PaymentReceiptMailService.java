package com.hireconnect.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.hireconnect.payment.config.MailProperties;
import com.hireconnect.payment.domain.PaymentTransaction;
import com.hireconnect.payment.domain.SubscriptionPlan;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class PaymentReceiptMailService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReceiptMailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
        .withZone(ZoneId.of("Asia/Kolkata"));

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final PaymentReceiptPdfService receiptPdfService;
    private final HttpMailService httpMailService;

    public PaymentReceiptMailService(
        JavaMailSender mailSender,
        MailProperties mailProperties,
        PaymentReceiptPdfService receiptPdfService,
        HttpMailService httpMailService
    ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.receiptPdfService = receiptPdfService;
        this.httpMailService = httpMailService;
    }

    public void sendSubscriptionReceipt(PaymentTransaction transaction, SubscriptionPlan plan) {
        if (!mailProperties.enabled() || !StringUtils.hasText(transaction.getCustomerEmail())) {
            return;
        }

        try {
            PaymentReceiptAttachment receipt = receiptPdfService.build(transaction, plan);
            if (httpMailService.enabled()) {
                if (!httpMailService.configured()) {
                    log.warn("Email API is not configured; subscription receipt for order {} was skipped", transaction.getOrderId());
                    return;
                }
                httpMailService.send(
                    transaction.getCustomerEmail(),
                    "HireConnect subscription receipt - " + plan.getDisplayName(),
                    buildPlainText(transaction, plan),
                    buildHtml(transaction, plan),
                    receipt
                );
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(transaction.getCustomerEmail());
            helper.setFrom(resolveFromAddress());
            helper.setSubject("HireConnect subscription receipt - " + plan.getDisplayName());
            helper.setText(buildPlainText(transaction, plan), buildHtml(transaction, plan));
            if (receipt.content() != null && receipt.content().length > 0) {
                helper.addAttachment(
                    receipt.filename(),
                    new ByteArrayResource(receipt.content()),
                    "application/pdf"
                );
            }
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            log.warn("Failed to send subscription receipt for order {}", transaction.getOrderId(), ex);
        }
    }

    private String resolveFromAddress() {
        return StringUtils.hasText(mailProperties.from()) ? mailProperties.from() : "no-reply@hireconnect.local";
    }

    private String buildPlainText(PaymentTransaction transaction, SubscriptionPlan plan) {
        return """
            HireConnect subscription receipt

            Plan: %s
            Role: %s
            Amount: %s %s
            Order ID: %s
            Payment ID: %s
            Start date: %s
            Expiry date: %s

            Your premium subscription is now active.
            """.formatted(
            plan.getDisplayName(),
            transaction.getRole(),
            transaction.getCurrency(),
            formatAmount(transaction.getAmount()),
            transaction.getOrderId(),
            safe(transaction.getTransactionId()),
            formatDate(transaction.getStartDate()),
            formatDate(transaction.getExpiryDate())
        );
    }

    private String buildHtml(PaymentTransaction transaction, SubscriptionPlan plan) {
        return """
            <!doctype html>
            <html>
              <body style="margin:0;background:#f3f7fb;font-family:Arial,Helvetica,sans-serif;color:#102033;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f7fb;padding:32px 0;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="width:620px;max-width:92%%;background:#ffffff;border:1px solid #dbe7f3;border-radius:18px;overflow:hidden;">
                        <tr>
                          <td style="background:#06172b;padding:28px 32px;color:#ffffff;">
                            <div style="font-size:13px;letter-spacing:2px;text-transform:uppercase;color:#22c3ff;font-weight:700;">HireConnect Billing</div>
                            <h1 style="margin:10px 0 0;font-size:28px;line-height:1.2;">Subscription activated</h1>
                            <p style="margin:8px 0 0;color:#c8d7e8;">Your payment receipt and plan details are below.</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px 32px;">
                            <p style="margin:0 0 18px;font-size:16px;">Hi %s,</p>
                            <p style="margin:0 0 24px;color:#4c5c70;line-height:1.6;">Thank you for subscribing to HireConnect. Your %s plan is active and your workspace benefits are available now.</p>
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:separate;border-spacing:0 10px;">
                              %s
                            </table>
                            <div style="margin-top:24px;padding:18px;border-radius:14px;background:#eef8ff;border:1px solid #bfe7ff;">
                              <strong style="color:#075985;">Billing note</strong>
                              <p style="margin:8px 0 0;color:#3b5268;line-height:1.5;">Keep this email as your subscription receipt. For any billing support, quote your Order ID.</p>
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:20px 32px;background:#f8fbff;color:#6b7b8d;font-size:13px;">
                            HireConnect operating system - secure hiring workflows for candidates and recruiters.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted(
            escape(StringUtils.hasText(transaction.getCustomerName()) ? transaction.getCustomerName() : "there"),
            escape(plan.getDisplayName()),
            receiptRows(transaction, plan)
        );
    }

    private String receiptRows(PaymentTransaction transaction, SubscriptionPlan plan) {
        return row("Plan", plan.getDisplayName())
            + row("Role", transaction.getRole().name())
            + row("Amount paid", transaction.getCurrency() + " " + formatAmount(transaction.getAmount()))
            + row("Order ID", transaction.getOrderId())
            + row("Payment ID", safe(transaction.getTransactionId()))
            + row("Start date", formatDate(transaction.getStartDate()))
            + row("Expiry date", formatDate(transaction.getExpiryDate()));
    }

    private String row(String label, String value) {
        return """
            <tr>
              <td style="padding:14px 16px;background:#f7fafc;border:1px solid #e1eaf3;border-right:0;border-radius:12px 0 0 12px;color:#65758a;font-size:13px;">%s</td>
              <td style="padding:14px 16px;background:#f7fafc;border:1px solid #e1eaf3;border-left:0;border-radius:0 12px 12px 0;font-weight:700;color:#12223a;text-align:right;">%s</td>
            </tr>
            """.formatted(escape(label), escape(value));
    }

    private String formatDate(Instant instant) {
        return instant == null ? "Not available" : DATE_FORMATTER.format(instant);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "Not available";
    }

    private String escape(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
