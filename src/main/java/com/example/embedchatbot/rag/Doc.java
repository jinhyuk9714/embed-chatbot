// ===========================================
// File: server/src/main/java/com/example/embedchatbot/rag/Doc.java
// ===========================================
package com.example.embedchatbot.rag;

public class Doc {
    public final String id;
    public final String title;
    public final String url;
    public final String text;
    public final String locale;

    public Doc(String id, String title, String url, String text, String locale) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.text = text;
        this.locale = locale;
    }
}
