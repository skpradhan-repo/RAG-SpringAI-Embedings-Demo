package com.company.rag.service;

import com.company.rag.dto.RagDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    @Value("${rag.chunking.chunk-size:800}")
    private int chunkSize;

    @Value("${rag.chunking.chunk-overlap:100}")
    private int chunkOverlap;

    /**
     * Ingest raw text directly into the vector store.
     */
    public RagDto.IngestResponse ingestText(RagDto.IngestTextRequest request) {
        log.info("Ingesting raw text titled: {}", request.getTitle());

        Map<String, Object> metadata = new HashMap<>();
        if (request.getTitle() != null) metadata.put("title", request.getTitle());
        if (request.getCategory() != null) metadata.put("category", request.getCategory());
        if (request.getMetadata() != null) metadata.putAll(request.getMetadata());
        metadata.put("source", "raw_text");

        Document doc = new Document(request.getContent(), metadata);
        List<Document> splitDocs = splitDocuments(List.of(doc));

        // Embedding model (Nomic Embed) automatically embeds the documents via vectorStore.accept
        vectorStore.accept(splitDocs);

        log.info("Successfully ingested {} chunk(s) for title: {}", splitDocs.size(), request.getTitle());
        return RagDto.IngestResponse.builder()
                .status("SUCCESS")
                .chunksIngested(splitDocs.size())
                .documentName(request.getTitle() != null ? request.getTitle() : "raw_text")
                .build();
    }

    /**
     * Ingest uploaded company files (PDF, DOCX, TXT, MD, etc.) using Tika Document Reader.
     */
    public RagDto.IngestResponse ingestFile(MultipartFile file, String category) throws IOException {
        String filename = file.getOriginalFilename();
        log.info("Ingesting document file: {}, size: {} bytes", filename, file.getSize());

        Resource resource = file.getResource();
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();

        // Enrich metadata
        documents.forEach(doc -> {
            doc.getMetadata().put("filename", filename);
            doc.getMetadata().put("contentType", file.getContentType());
            if (category != null && !category.isBlank()) {
                doc.getMetadata().put("category", category);
            }
        });

        List<Document> splitDocs = splitDocuments(documents);

        // Compute embeddings and store in pgvector
        vectorStore.accept(splitDocs);

        log.info("Successfully ingested {} chunk(s) from file: {}", splitDocs.size(), filename);
        return RagDto.IngestResponse.builder()
                .status("SUCCESS")
                .chunksIngested(splitDocs.size())
                .documentName(filename)
                .build();
    }

    /**
     * Chunk documents for high-accuracy embedding & similarity search.
     */
    private List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,
                chunkOverlap,
                5,
                10000,
                true
        );
        return splitter.apply(documents);
    }
}
