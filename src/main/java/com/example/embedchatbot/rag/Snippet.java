// ==============================================
// File: server/src/main/java/com/example/embedchatbot/rag/Snippet.java
// ==============================================
package com.example.embedchatbot.rag;

public class Snippet {
    public final String title;
    public final String url;
    public final String text;
    public final double score;

    public Snippet(String title, String url, String text, double score) {
        this.title = title;
        this.url = url;
        this.text = text;
        this.score = score;
    }
}
