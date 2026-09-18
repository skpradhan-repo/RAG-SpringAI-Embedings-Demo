package com.company.rag.service;

import com.company.rag.dto.RagDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagQueryService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("${rag.retrieval.top-k:4}")
    private int defaultTopK;

    @Value("${rag.retrieval.similarity-threshold:0.60}")
    private double defaultSimilarityThreshold;

    public RagQueryService(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.create(chatModel);
    }

    private static final String RAG_PROMPT_TEMPLATE = """
            You are a helpful and accurate assistant answering questions using only the provided reference context.
            
            ---------------------
            CONTEXT:
            {context}
            ---------------------
            
            USER QUESTION:
            {question}
            
            INSTRUCTIONS:
            - Answer the question factually based ONLY on the provided context.
            - If the context does not contain enough information to answer the question, state: "I cannot find sufficient information in the provided company documents to answer this question."
            - Do not invent or assume facts not present in the context.
            - Cite references or metadata if relevant.
            """;

    public RagDto.AnswerResponse askQuestion(RagDto.QuestionRequest request) {
        String question = request.getQuestion();
        int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
        double threshold = request.getSimilarityThreshold() != null ? request.getSimilarityThreshold() : defaultSimilarityThreshold;

        log.info("Processing RAG query: '{}' (topK={}, threshold={})", question, topK, threshold);

        // 1. Retrieve similar documents from pgvector (similarity search)
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
        log.info("Found {} relevant documents matching query", similarDocs.size());

        // 2. Format Context
        String context = similarDocs.isEmpty()
                ? "No matching documents found in the database."
                : similarDocs.stream()
                .map(doc -> "- " + doc.getText().trim())
                .collect(Collectors.joining("\n\n"));

        // 3. Build Prompt & Invoke LLM
        PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        String renderedPrompt = promptTemplate.render(Map.of(
                "context", context,
                "question", question
        ));

        String answer = chatClient.prompt()
                .user(renderedPrompt)
                .call()
                .content();

        // 4. Map Source Documents
        List<RagDto.SourceDocument> sourceDocuments = similarDocs.stream()
                .map(doc -> RagDto.SourceDocument.builder()
                        .id(doc.getId())
                        .content(doc.getText())
                        .score(doc.getScore())
                        .metadata(doc.getMetadata())
                        .build())
                .toList();

        return RagDto.AnswerResponse.builder()
                .question(question)
                .answer(answer)
                .sources(sourceDocuments)
                .build();
    }
}
