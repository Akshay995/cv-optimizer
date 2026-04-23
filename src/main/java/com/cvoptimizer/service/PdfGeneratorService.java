package com.cvoptimizer.service;

import com.cvoptimizer.model.CvData;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class PdfGeneratorService {

    // Color palette
    private static final Color HEADER_BG    = new DeviceRgb(15,  23,  42);   // navy
    private static final Color ACCENT       = new DeviceRgb(56,  189, 248);   // sky blue
    private static final Color SECTION_CLR  = new DeviceRgb(14,  165, 233);   // blue
    private static final Color TEXT_DARK    = new DeviceRgb(15,  23,  42);
    private static final Color TEXT_BODY    = new DeviceRgb(51,  65,  85);
    private static final Color TEXT_MUTED   = new DeviceRgb(100, 116, 139);
    private static final Color CHIP_BG      = new DeviceRgb(224, 242, 254);   // light blue chip
    private static final Color CHIP_TEXT    = new DeviceRgb(3,   105, 161);
    private static final Color DIVIDER_CLR  = new DeviceRgb(203, 213, 225);
    private static final Color BULLET_CLR   = new DeviceRgb(56,  189, 248);

    public byte[] generate(CvData cv) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(0, 0, 20, 0);

        PdfFont regular  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont bold     = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont italic   = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
        PdfFont boldItal = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLDOBLIQUE);

        buildHeader(doc, cv, bold, regular, italic);
        doc.setMargins(0, 36, 20, 36);

        if (hasText(cv.getSummary())) {
            buildSummarySection(doc, cv.getSummary(), bold, regular, italic);
        }

        if (cv.getSkills() != null && !cv.getSkills().isEmpty()) {
            buildSkillsSection(doc, cv.getSkills(), bold, regular);
        }

        if (cv.getExperience() != null && !cv.getExperience().isEmpty()) {
            buildExperienceSection(doc, cv.getExperience(), bold, regular, italic, boldItal);
        }

        if (cv.getProjects() != null && !cv.getProjects().isEmpty()) {
            buildProjectsSection(doc, cv.getProjects(), bold, regular, italic);
        }

        if (cv.getEducation() != null && !cv.getEducation().isEmpty()) {
            buildEducationSection(doc, cv.getEducation(), bold, regular, italic);
        }

        if (cv.getCertifications() != null && !cv.getCertifications().isEmpty()) {
            buildCertificationsSection(doc, cv.getCertifications(), bold, regular);
        }

        doc.close();
        return baos.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Header
    // -----------------------------------------------------------------------
    private void buildHeader(Document doc, CvData cv, PdfFont bold, PdfFont regular, PdfFont italic) throws IOException {
        // Full-bleed header background
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(HEADER_BG)
                .setPadding(28)
                .setMargin(0);

        // Left: name + title
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(HEADER_BG);

        String name = cv.getName() != null ? cv.getName() : "Your Name";
        leftCell.add(new Paragraph(name)
                .setFont(bold).setFontSize(26).setFontColor(new DeviceRgb(248, 250, 252))
                .setMarginBottom(4));

        if (hasText(cv.getTitle())) {
            leftCell.add(new Paragraph(cv.getTitle())
                    .setFont(italic).setFontSize(10).setFontColor(ACCENT)
                    .setMarginBottom(0));
        }

        headerTable.addCell(leftCell);

        // Right: contact info
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(HEADER_BG)
                .setTextAlignment(TextAlignment.RIGHT);

        addContactLine(rightCell, cv.getEmail(), regular);
        addContactLine(rightCell, cv.getPhone(), regular);
        addContactLine(rightCell, cv.getLinkedin(), regular);
        addContactLine(rightCell, cv.getGithub(), regular);
        addContactLine(rightCell, cv.getWebsite(), regular);

        headerTable.addCell(rightCell);

        // Accent bottom border on header
        Table accentBar = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(ACCENT)
                .setMargin(0);
        accentBar.addCell(new Cell().setHeight(3).setBorder(Border.NO_BORDER)
                .setBackgroundColor(ACCENT));

        doc.add(headerTable);
        doc.add(accentBar);
        doc.add(new Paragraph("").setMarginBottom(6));
    }

    private void addContactLine(Cell cell, String value, PdfFont font) {
        if (!hasText(value)) return;
        cell.add(new Paragraph(value)
                .setFont(font).setFontSize(8.5f).setFontColor(new DeviceRgb(203, 213, 225))
                .setMarginBottom(2));
    }

    // -----------------------------------------------------------------------
    // Section header helper
    // -----------------------------------------------------------------------
    private void addSectionHeader(Document doc, String title, PdfFont bold) {
        doc.add(new Paragraph("").setMarginTop(10));
        doc.add(new Paragraph(title.toUpperCase())
                .setFont(bold).setFontSize(9).setFontColor(SECTION_CLR)
                .setCharacterSpacing(1.5f)
                .setMarginBottom(3));
        Table rule = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMargin(0);
        rule.addCell(new Cell().setHeight(1.5f).setBackgroundColor(SECTION_CLR)
                .setBorder(Border.NO_BORDER));
        doc.add(rule);
        doc.add(new Paragraph("").setMarginBottom(5));
    }

    // -----------------------------------------------------------------------
    // Summary
    // -----------------------------------------------------------------------
    private void buildSummarySection(Document doc, String summary, PdfFont bold, PdfFont regular, PdfFont italic) {
        addSectionHeader(doc, "Professional Summary", bold);
        doc.add(new Paragraph(summary)
                .setFont(regular).setFontSize(9.5f).setFontColor(TEXT_BODY)
                .setFixedLeading(14).setMarginBottom(4));
    }

    // -----------------------------------------------------------------------
    // Skills
    // -----------------------------------------------------------------------
    private void buildSkillsSection(Document doc, Map<String, List<String>> skills, PdfFont bold, PdfFont regular) {
        addSectionHeader(doc, "Technical Skills", bold);

        for (Map.Entry<String, List<String>> entry : skills.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;

            Table row = new Table(UnitValue.createPercentArray(new float[]{18, 82}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(5);

            Cell labelCell = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(4);
            labelCell.add(new Paragraph(entry.getKey())
                    .setFont(bold).setFontSize(8.5f).setFontColor(TEXT_DARK));
            row.addCell(labelCell);

            // Chips container
            Cell chipsCell = new Cell().setBorder(Border.NO_BORDER);
            Paragraph chipsPara = new Paragraph().setMultipliedLeading(1.6f);
            for (String skill : entry.getValue()) {
                Text chip = new Text("  " + skill + "  ")
                        .setFont(regular).setFontSize(7.5f)
                        .setFontColor(CHIP_TEXT)
                        .setBackgroundColor(CHIP_BG, 2f, 2f, 2f, 2f);
                chipsPara.add(chip).add(new Text("  ").setFont(regular).setFontSize(7.5f));
            }
            chipsCell.add(chipsPara);
            row.addCell(chipsCell);

            doc.add(row);
        }
    }

    // -----------------------------------------------------------------------
    // Experience
    // -----------------------------------------------------------------------
    private void buildExperienceSection(Document doc, List<CvData.Experience> experiences,
                                        PdfFont bold, PdfFont regular, PdfFont italic, PdfFont boldItal) {
        addSectionHeader(doc, "Professional Experience", bold);

        for (CvData.Experience exp : experiences) {
            // Company + Duration row
            Table companyRow = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(0);

            Cell companyCell = new Cell().setBorder(Border.NO_BORDER);
            if (hasText(exp.getCompany())) {
                companyCell.add(new Paragraph(exp.getCompany())
                        .setFont(bold).setFontSize(10).setFontColor(TEXT_DARK).setMarginBottom(1));
            }
            companyRow.addCell(companyCell);

            Cell durationCell = new Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            if (hasText(exp.getDuration())) {
                durationCell.add(new Paragraph(exp.getDuration())
                        .setFont(italic).setFontSize(8.5f).setFontColor(TEXT_MUTED));
            }
            companyRow.addCell(durationCell);
            doc.add(companyRow);

            // Role + location
            Table roleRow = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(3);

            Cell roleCell = new Cell().setBorder(Border.NO_BORDER);
            if (hasText(exp.getRole())) {
                roleCell.add(new Paragraph(exp.getRole())
                        .setFont(boldItal).setFontSize(9).setFontColor(SECTION_CLR).setMarginBottom(0));
            }
            roleRow.addCell(roleCell);

            Cell locCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            if (hasText(exp.getLocation())) {
                locCell.add(new Paragraph(exp.getLocation())
                        .setFont(italic).setFontSize(8f).setFontColor(TEXT_MUTED));
            }
            roleRow.addCell(locCell);
            doc.add(roleRow);

            // Bullet points
            if (exp.getBullets() != null) {
                for (String bullet : exp.getBullets()) {
                    if (!hasText(bullet)) continue;
                    Table bulletRow = new Table(UnitValue.createPercentArray(new float[]{3, 97}))
                            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(1);

                    Cell dotCell = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingLeft(6);
                    dotCell.add(new Paragraph("-").setFont(bold).setFontSize(9).setFontColor(BULLET_CLR));
                    bulletRow.addCell(dotCell);

                    Cell txtCell = new Cell().setBorder(Border.NO_BORDER);
                    txtCell.add(new Paragraph(bullet)
                            .setFont(regular).setFontSize(9).setFontColor(TEXT_BODY).setFixedLeading(13));
                    bulletRow.addCell(txtCell);

                    doc.add(bulletRow);
                }
            }
            doc.add(new Paragraph("").setMarginBottom(6));
        }
    }

    // -----------------------------------------------------------------------
    // Projects
    // -----------------------------------------------------------------------
    private void buildProjectsSection(Document doc, List<CvData.Project> projects,
                                      PdfFont bold, PdfFont regular, PdfFont italic) {
        addSectionHeader(doc, "Projects", bold);

        for (CvData.Project project : projects) {
            // Project name + tech row
            Table nameRow = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(1);

            Cell nameCell = new Cell().setBorder(Border.NO_BORDER);
            if (hasText(project.getName())) {
                nameCell.add(new Paragraph(project.getName())
                        .setFont(bold).setFontSize(9.5f).setFontColor(TEXT_DARK));
            }
            nameRow.addCell(nameCell);

            Cell techCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            if (project.getTech() != null && !project.getTech().isEmpty()) {
                techCell.add(new Paragraph(String.join(" | ", project.getTech()))
                        .setFont(italic).setFontSize(7.5f).setFontColor(SECTION_CLR));
            }
            nameRow.addCell(techCell);
            doc.add(nameRow);

            if (hasText(project.getDescription())) {
                doc.add(new Paragraph(project.getDescription())
                        .setFont(regular).setFontSize(9).setFontColor(TEXT_BODY)
                        .setFixedLeading(13).setMarginBottom(2).setMarginLeft(8));
            }

            if (hasText(project.getLink())) {
                doc.add(new Paragraph(project.getLink())
                        .setFont(italic).setFontSize(8).setFontColor(ACCENT)
                        .setMarginBottom(2).setMarginLeft(8));
            }

            doc.add(new Paragraph("").setMarginBottom(4));
        }
    }

    // -----------------------------------------------------------------------
    // Education
    // -----------------------------------------------------------------------
    private void buildEducationSection(Document doc, List<CvData.Education> educations,
                                       PdfFont bold, PdfFont regular, PdfFont italic) {
        addSectionHeader(doc, "Education", bold);

        for (CvData.Education edu : educations) {
            Table row = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(1);

            Cell leftCell = new Cell().setBorder(Border.NO_BORDER);
            if (hasText(edu.getDegree())) {
                leftCell.add(new Paragraph(edu.getDegree())
                        .setFont(bold).setFontSize(9.5f).setFontColor(TEXT_DARK).setMarginBottom(1));
            }
            if (hasText(edu.getInstitution())) {
                leftCell.add(new Paragraph(edu.getInstitution())
                        .setFont(regular).setFontSize(9).setFontColor(TEXT_BODY));
            }
            row.addCell(leftCell);

            Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            if (hasText(edu.getYear())) {
                rightCell.add(new Paragraph(edu.getYear())
                        .setFont(italic).setFontSize(8.5f).setFontColor(TEXT_MUTED));
            }
            if (hasText(edu.getGpa())) {
                rightCell.add(new Paragraph("GPA: " + edu.getGpa())
                        .setFont(regular).setFontSize(8.5f).setFontColor(SECTION_CLR));
            }
            row.addCell(rightCell);
            doc.add(row);
            doc.add(new Paragraph("").setMarginBottom(5));
        }
    }

    // -----------------------------------------------------------------------
    // Certifications
    // -----------------------------------------------------------------------
    private void buildCertificationsSection(Document doc, List<String> certifications, PdfFont bold, PdfFont regular) {
        addSectionHeader(doc, "Certifications", bold);

        for (String cert : certifications) {
            if (!hasText(cert)) continue;
            Table row = new Table(UnitValue.createPercentArray(new float[]{3, 97}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(2);

            Cell dotCell = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(3).setPaddingLeft(6);
            dotCell.add(new Paragraph("-").setFont(bold).setFontSize(9).setFontColor(BULLET_CLR));
            row.addCell(dotCell);

            Cell txtCell = new Cell().setBorder(Border.NO_BORDER);
            txtCell.add(new Paragraph(cert).setFont(regular).setFontSize(9).setFontColor(TEXT_BODY));
            row.addCell(txtCell);

            doc.add(row);
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
