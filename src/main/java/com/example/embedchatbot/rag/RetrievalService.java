// ===================================================
// File: server/src/main/java/com/example/embedchatbot/rag/RetrievalService.java
// ===================================================
package com.example.embedchatbot.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final RagProperties props;
    private final MarkdownLoader loader = new MarkdownLoader();
    private volatile RagIndex index = new RagIndex(List.of());

    public RetrievalService(RagProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void build() {
        if (!props.isEnabled()) { log.info("RAG disabled"); return; }
        try {
            if (!Files.exists(Path.of(props.getDocsPath()))) {
                log.warn("RAG docs path not found: {}", props.getDocsPath());
                return;
            }
            List<Doc> docs = loader.load(props.getDocsPath(), props.getMaxDocChars(),
                    props.getChunkSize(), props.getChunkOverlap());
            this.index = new RagIndex(docs);
            log.info("RAG index built: {} chunks from {}", docs.size(), props.getDocsPath());
        } catch (Exception e) {
            log.error("RAG build failed", e);
        }
    }

    public List<Snippet> retrieve(String query, String locale, Integer k) {
        if (!props.isEnabled() || query == null || query.isBlank()) {
            return List.of();
        }
        int topK = (k != null && k > 0) ? k : props.getTopK();
        return index.retrieve(query, locale, topK);
    }
}
