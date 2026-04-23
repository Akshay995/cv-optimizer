package com.cvoptimizer.controller;

import com.cvoptimizer.model.CvData;
import com.cvoptimizer.service.CvEnhancerService;
import com.cvoptimizer.service.CvParserService;
import com.cvoptimizer.service.PdfGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "CV Optimizer", description = "Upload a CV and receive an AI-enhanced, ATS-optimized PDF")
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

    @Operation(
        summary = "Optimize a CV",
        description = "Upload a PDF, DOCX, or TXT CV. Optionally provide a job description so Claude AI can tailor the CV for ATS scores above 90%."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Optimized CV PDF", content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "Invalid input (empty file, unsupported format, or file too large)", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Processing error", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeCv(
            @Parameter(description = "CV file (PDF, DOCX, or TXT, max 5 MB)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target job role to tailor the CV toward (optional)")
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @Parameter(description = "Full job description — used to align keywords and achieve ATS score above 90% (optional)")
            @RequestParam(value = "jobDescription", required = false) String jobDescription) {
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

            if (targetRole != null && !targetRole.isBlank()) {
                rawText = "Target Role: " + targetRole.trim() + "\n\n" + rawText;
            }

            log.info("Enhancing CV via Claude API (JD provided: {})...", jobDescription != null && !jobDescription.isBlank());
            CvData enhancedCv = enhancerService.enhance(rawText, jobDescription);

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
