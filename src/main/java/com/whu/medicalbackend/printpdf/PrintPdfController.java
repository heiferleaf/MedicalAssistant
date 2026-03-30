package com.whu.medicalbackend.printpdf;

import jakarta.servlet.http.HttpServletRequest;
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
    public PrintPdfResponse generatePdf(@RequestBody PrintPdfRequest request, HttpServletRequest httpRequest) {
        try {
            // service 返回相对路径，例如 /api/medical/prepare/pdf/file/xxx.pdf
            String relativeUrl = printPdfService.generatePdf(request);

            // 统一转为绝对 URL，前端直接使用
            String fileUrl = buildAbsoluteUrl(httpRequest, relativeUrl);

            return new PrintPdfResponse(true, "PDF生成成功", fileUrl);
        } catch (Exception e) {
            return new PrintPdfResponse(false, "PDF生成失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getPdfFile(@PathVariable String fileName) {
        File file = new File("uploads/printpdf", fileName);
        System.out.println("[PDF] try read: " + file.getAbsolutePath() + ", exists=" + file.exists());

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private String buildAbsoluteUrl(HttpServletRequest request, String relativeUrl) {
        String path = (relativeUrl == null || relativeUrl.isBlank()) ? "" : relativeUrl.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // 兼容反向代理头
        String proto = headerOrDefault(request, "X-Forwarded-Proto", request.getScheme());
        String host = headerOrDefault(request, "X-Forwarded-Host", request.getServerName());
        String portHeader = request.getHeader("X-Forwarded-Port");

        int port = request.getServerPort();
        if (portHeader != null && !portHeader.isBlank()) {
            try { port = Integer.parseInt(portHeader); } catch (Exception ignored) {}
        }

        boolean defaultPort = ("http".equalsIgnoreCase(proto) && port == 80)
                || ("https".equalsIgnoreCase(proto) && port == 443);

        StringBuilder base = new StringBuilder();
        base.append(proto).append("://").append(host);

        // host 里已带端口时不重复拼
        if (!host.contains(":") && !defaultPort) {
            base.append(":").append(port);
        }

        return base + path;
    }

    private String headerOrDefault(HttpServletRequest request, String header, String def) {
        String v = request.getHeader(header);
        return (v == null || v.isBlank()) ? def : v;
    }
}