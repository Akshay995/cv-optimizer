package com.cvoptimizer.controller;

import com.cvoptimizer.model.CvData;
import com.cvoptimizer.service.CvEnhancerService;
import com.cvoptimizer.service.CvParserService;
import com.cvoptimizer.service.PdfGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class CvController {

    private static final Logger log = LoggerFactory.getLogger(CvController.class);

    private final CvParserService parserService;
    private final CvEnhancerService enhancerService;
    private final PdfGeneratorService pdfGeneratorService;

    public CvController(CvParserService parserService,
                        CvEnhancerService enhancerService,
                        PdfGeneratorService pdfGeneratorService) {
        this.parserService = parserService;
        this.enhancerService = enhancerService;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeCv(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "targetRole", required = false) String targetRole) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Please upload a file.\"}");
        }

        long maxSizeBytes = 5 * 1024 * 1024; // 5 MB
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"File size must be under 5 MB.\"}");
        }

        try {
            log.info("Parsing CV: {}", file.getOriginalFilename());
            String rawText = parserService.extractText(file);

            if (rawText == null || rawText.isBlank()) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Could not extract text from the uploaded file.\"}");
            }

            // Append target role context if provided
            if (targetRole != null && !targetRole.isBlank()) {
                rawText = "Target Role: " + targetRole.trim() + "\n\n" + rawText;
            }

            log.info("Enhancing CV via Claude API...");
            CvData enhancedCv = enhancerService.enhance(rawText);

            log.info("Generating PDF for: {}", enhancedCv.getName());
            byte[] pdfBytes = pdfGeneratorService.generate(enhancedCv);

            String safeName = (enhancedCv.getName() != null)
                    ? enhancedCv.getName().replaceAll("[^a-zA-Z0-9]", "_")
                    : "Enhanced";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + safeName + "_Enhanced_CV.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(new ByteArrayResource(pdfBytes));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            log.error("CV optimization failed", e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Processing failed: " + e.getMessage() + "\"}");
        }
    }
}
