package com.cvoptimizer.service;

import com.cvoptimizer.model.CvData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CvEnhancerService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.model:claude-sonnet-4-6}")
    private String model;

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public CvData enhance(String rawCvText) throws Exception {
        String prompt = buildPrompt(rawCvText);
        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API error " + response.statusCode() + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    private String buildPrompt(String rawCvText) {
        return """
                You are an expert technical resume writer specializing in software engineering, \
                data science, AI/ML, DevOps, and cloud roles. Your task is to enhance a CV to be \
                maximally tech-specific, ATS-optimized, and compelling for top-tier tech companies.

                Instructions:
                1. Extract ALL information from the provided CV.
                2. Rewrite bullet points using strong action verbs (Engineered, Architected, Optimized, Deployed, etc.) \
                and add quantified impact wherever reasonable (e.g., "reduced latency by 35%", "served 10K+ users").
                3. Make the professional summary compelling, tech-focused, and 2-3 sentences max.
                4. Organize skills into clean categories (Languages, Frameworks, Databases, Cloud & DevOps, Tools).
                5. Keep all real information intact - do not invent facts, only enhance wording.
                6. Make project descriptions highlight technical depth and impact.

                Return ONLY valid JSON - no markdown, no explanation, just raw JSON:
                {
                  "name": "Full Name",
                  "email": "email@example.com",
                  "phone": "+91-XXXXXXXXXX",
                  "linkedin": "linkedin.com/in/profile",
                  "github": "github.com/username",
                  "website": "portfolio.com",
                  "title": "Software Engineer | Java | Spring Boot | AWS",
                  "summary": "Compelling 2-3 sentence professional summary here.",
                  "skills": {
                    "Languages": ["Java", "Python", "JavaScript"],
                    "Frameworks": ["Spring Boot", "React", "Django"],
                    "Databases": ["MySQL", "MongoDB", "PostgreSQL"],
                    "Cloud & DevOps": ["AWS", "Docker", "Kubernetes", "CI/CD"],
                    "Tools": ["Git", "Jira", "IntelliJ IDEA"]
                  },
                  "experience": [
                    {
                      "company": "Company Name",
                      "role": "Software Engineer",
                      "duration": "Jan 2022 - Present",
                      "location": "Bangalore, India",
                      "bullets": [
                        "Engineered a microservices-based order management system using Spring Boot and Kafka, processing 50K+ transactions/day.",
                        "Optimized SQL query performance by 40% through strategic indexing and query refactoring.",
                        "Led migration from monolithic architecture to AWS ECS, reducing deployment time by 60%."
                      ]
                    }
                  ],
                  "projects": [
                    {
                      "name": "Project Name",
                      "description": "Tech-specific description highlighting architecture, scale, and impact.",
                      "tech": ["Java", "Spring Boot", "MySQL", "Docker"],
                      "link": "github.com/username/project"
                    }
                  ],
                  "education": [
                    {
                      "institution": "University Name",
                      "degree": "B.Tech in Computer Science & Engineering",
                      "year": "2020 - 2024",
                      "gpa": "8.7/10"
                    }
                  ],
                  "certifications": [
                    "AWS Certified Developer - Associate (2023)",
                    "Oracle Certified Professional Java SE 11 (2022)"
                  ]
                }

                CV to enhance:
                """ + rawCvText;
    }

    private String buildRequestBody(String prompt) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);

        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        body.put("messages", List.of(message));

        return objectMapper.writeValueAsString(body);
    }

    private CvData parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("content").get(0).path("text").asText();

        // Strip markdown code fences if Claude wrapped the JSON
        content = content.strip();
        if (content.startsWith("```")) {
            content = content.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
        }

        JsonNode cvJson = objectMapper.readTree(content);
        CvData cvData = objectMapper.treeToValue(cvJson, CvData.class);

        // Normalize skills: handle case where Claude returns flat array instead of map
        if (cvData.getSkills() == null && cvJson.has("skills") && cvJson.get("skills").isArray()) {
            Map<String, List<String>> skillMap = new LinkedHashMap<>();
            List<String> allSkills = new ArrayList<>();
            cvJson.get("skills").forEach(s -> allSkills.add(s.asText()));
            skillMap.put("Technical Skills", allSkills);
            cvData.setSkills(skillMap);
        }

        return cvData;
    }
}
