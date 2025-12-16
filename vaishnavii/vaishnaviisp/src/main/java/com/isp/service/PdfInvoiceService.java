package com.isp.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service for generating professional PDF invoices.
 */
public class PdfInvoiceService {

    /**
     * Generate a professional invoice PDF.
     */
    public byte[] generateInvoice(
            String invoiceId,
            String customerName,
            String customerEmail,
            String planName,
            double monthlyCharge,
            double totalUsageGB,
            double totalAmount) throws IOException {

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float margin = 40;
                float y = pageHeight - margin;

                // Header with company info
                cs.setNonStrokingColor(33, 150, 243);
                cs.addRect(0, y - 60, pageWidth, 60);
                cs.fill();

                cs.setNonStrokingColor(255, 255, 255);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
                cs.newLineAtOffset(margin, y - 40);
                cs.showText("ISP MANAGEMENT");
                cs.endText();

                // Invoice title
                y -= 80;
                cs.setNonStrokingColor(0, 0, 0);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText("INVOICE");
                cs.endText();

                // Invoice details
                y -= 30;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice ID: " + invoiceId);
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                cs.endText();

                // Bill To section
                y -= 40;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("BILL TO:");
                cs.endText();

                y -= 20;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText(customerName);
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Email: " + customerEmail);
                cs.endText();

                // Items table header
                y -= 40;
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();

                y -= 5;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Description");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(pageWidth - margin - 100, y);
                cs.showText("Amount");
                cs.endText();

                // Items
                y -= 25;
                cs.setLineWidth(0.25f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();

                y -= 20;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Plan: " + planName);
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(pageWidth - margin - 100, y);
                cs.showText(String.format(Locale.US, "$%.2f", monthlyCharge));
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Data Usage: " + String.format(Locale.US, "%.2f GB", totalUsageGB));
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(pageWidth - margin - 100, y);
                cs.showText("$0.00");
                cs.endText();

                // Total
                y -= 30;
                cs.setLineWidth(1.0f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();

                y -= 5;
                cs.setNonStrokingColor(33, 150, 243);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(pageWidth - margin - 150, y);
                cs.showText("Total Due:");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(pageWidth - margin - 100, y);
                cs.showText(String.format(Locale.US, "$%.2f", totalAmount));
                cs.endText();

                // Footer
                y = 50;
                cs.setNonStrokingColor(128, 128, 128);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Thank you for your business. Payment is due within 30 days. For inquiries, contact support@ispmanagement.com");
                cs.endText();
            }

            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
