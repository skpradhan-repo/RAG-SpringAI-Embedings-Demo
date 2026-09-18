package com.company.rag.controller;

import com.company.rag.dto.RagDto;
import com.company.rag.service.DocumentIngestionService;
import com.company.rag.service.RagQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final RagQueryService queryService;

    /**
     * Ingest raw text or JSON data directly into the pgvector knowledge store.
     */
    @PostMapping("/ingest/text")
    public ResponseEntity<RagDto.IngestResponse> ingestText(@Valid @RequestBody RagDto.IngestTextRequest request) {
        RagDto.IngestResponse response = ingestionService.ingestText(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ingest company files (PDF, DOCX, TXT, MD, etc.).
     */
    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RagDto.IngestResponse> ingestFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category) throws IOException {
        RagDto.IngestResponse response = ingestionService.ingestFile(file, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ask a question using the full RAG pipeline (Similarity search -> Context + Prompt -> LLM response).
     */
    @PostMapping("/ask")
    public ResponseEntity<RagDto.AnswerResponse> askQuestion(@Valid @RequestBody RagDto.QuestionRequest request) {
        RagDto.AnswerResponse response = queryService.askQuestion(request);
        return ResponseEntity.ok(response);
    }
}
