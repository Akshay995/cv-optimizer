package com.cvoptimizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CvData {

    private String name;
    private String email;
    private String phone;
    private String linkedin;
    private String github;
    private String website;
    private String title;
    private String summary;

    private Map<String, List<String>> skills;

    private List<Experience> experience;
    private List<Project> projects;
    private List<Education> education;
    private List<String> certifications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Experience {
        private String company;
        private String role;
        private String duration;
        private String location;
        private List<String> bullets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        private String name;
        private String description;
        private List<String> tech;
        private String link;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Education {
        private String institution;
        private String degree;
        private String year;
        private String gpa;
    }
}
