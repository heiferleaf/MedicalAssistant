package com.whu.medicalbackend.printpdf;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/medical/prepare/pdf")
public class PrintPdfController {

    private final PrintPdfService printPdfService;
    private static final String OUTPUT_DIR = "uploads/printpdf";

    public PrintPdfController(PrintPdfService printPdfService) {
        this.printPdfService = printPdfService;
    }

    @PostMapping
    public PrintPdfResponse generatePdf(@RequestBody PrintPdfRequest request) {
        try {
            String fileUrl = printPdfService.generatePdf(request);
            return new PrintPdfResponse(true, "PDF生成成功", fileUrl);
        } catch (Exception e) {
            return new PrintPdfResponse(false, "PDF生成失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getPdfFile(@PathVariable String fileName) {
        File file = new File(OUTPUT_DIR, fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}