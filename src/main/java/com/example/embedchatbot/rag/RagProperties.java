// ==============================
// File: server/src/main/java/com/example/embedchatbot/rag/RagProperties.java
// ==============================
package com.example.embedchatbot.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private boolean enabled = true;
    private int topK = 3;
    private String docsPath = "data/docs"; // 프로젝트 루트 기준
    private int maxDocChars = 8000;        // 너무 큰 문서 방지
    private int chunkSize = 800;           // 문자 기준 청크
    private int chunkOverlap = 120;

    // getters/setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public String getDocsPath() { return docsPath; }
    public void setDocsPath(String docsPath) { this.docsPath = docsPath; }
    public int getMaxDocChars() { return maxDocChars; }
    public void setMaxDocChars(int maxDocChars) { this.maxDocChars = maxDocChars; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
