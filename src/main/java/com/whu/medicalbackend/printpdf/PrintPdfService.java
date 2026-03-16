package com.whu.medicalbackend.printpdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

@Service
public class PrintPdfService {

    private static final String OUTPUT_DIR = "uploads/printpdf";

    public String generatePdf(PrintPdfRequest request) throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "prepare_" + UUID.randomUUID() + ".pdf";
        File file = new File(dir, fileName);

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        BaseFont baseFont = BaseFont.createFont(
                "STSong-Light",
                "UniGB-UCS2-H",
                BaseFont.NOT_EMBEDDED
        );

        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font sectionFont = new Font(baseFont, 14, Font.BOLD);
        Font textFont = new Font(baseFont, 11, Font.NORMAL);

        Paragraph title = new Paragraph("就医准备单", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        addLine(document, "生成时间：" + nvl(request.getGeneratedTime()), textFont);
        addLine(document, "患者：" + nvl(request.getPatient()), textFont);
        addLine(document, "就诊科室：" + nvl(request.getDepartment()), textFont);
        addLine(document, "就诊日期：" + nvl(request.getVisitDate()), textFont);

        addSectionTitle(document, "一、近期用药清单", sectionFont);
        List<PrintPdfRequest.MedicationItem> medications = request.getMedications();
        if (medications == null || medications.isEmpty()) {
            addLine(document, "无", textFont);
        } else {
            for (PrintPdfRequest.MedicationItem item : medications) {
                addLine(
                        document,
                        "- " + nvl(item.getName()) +
                                " | " + nvl(item.getSchedule()) +
                                " | " + (item.getTakenDays() == null ? 0 : item.getTakenDays()) + "天" +
                                " | 漏服" + (item.getMissedCount() == null ? 0 : item.getMissedCount()) + "次" +
                                " | " + nvl(item.getStatus()),
                        textFont
                );
            }
        }

        addSectionTitle(document, "二、异常健康数据", sectionFont);
        List<PrintPdfRequest.HealthDataItem> healthData = request.getHealthData();
        if (healthData == null || healthData.isEmpty()) {
            addLine(document, "无", textFont);
        } else {
            for (PrintPdfRequest.HealthDataItem item : healthData) {
                addLine(
                        document,
                        "- " + nvl(item.getDate()) +
                                " | " + nvl(item.getIndicator()) +
                                " | " + nvl(item.getValue()) + " " + nvl(item.getUnit()) +
                                " | " + nvl(item.getStatus()),
                        textFont
                );
            }
        }

        addSectionTitle(document, "三、待咨询问题", sectionFont);
        List<String> questions = request.getQuestions();
        if (questions == null || questions.isEmpty()) {
            addLine(document, "无", textFont);
        } else {
            for (int i = 0; i < questions.size(); i++) {
                addLine(document, (i + 1) + ". " + nvl(questions.get(i)), textFont);
            }
        }

        addSectionTitle(document, "四、其他信息", sectionFont);
        addLine(document, nvl(request.getOtherInfo()).isEmpty() ? "无" : nvl(request.getOtherInfo()), textFont);

        Paragraph footer = new Paragraph("仅供就医参考，请以医生诊断为准。", textFont);
        footer.setSpacingBefore(20);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        return "/api/medical/prepare/pdf/file/" + fileName;
    }

    private void addSectionTitle(Document document, String text, Font font) throws Exception {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private void addLine(Document document, String text, Font font) throws Exception {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setLeading(18);
        paragraph.setSpacingAfter(4);
        document.add(paragraph);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}