package com.company.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class RagDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestTextRequest {
        @NotBlank(message = "Content cannot be blank")
        private String content;

        private String title;
        private String category;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestResponse {
        private String status;
        private int chunksIngested;
        private String documentName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionRequest {
        @NotBlank(message = "Question cannot be blank")
        private String question;

        private Integer topK;
        private Double similarityThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResponse {
        private String question;
        private String answer;
        private List<SourceDocument> sources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceDocument {
        private String id;
        private String content;
        private Double score;
        private Map<String, Object> metadata;
    }
}
