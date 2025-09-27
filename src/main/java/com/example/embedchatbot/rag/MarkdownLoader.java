// =====================================================
// File: server/src/main/java/com/example/embedchatbot/rag/MarkdownLoader.java
// =====================================================
package com.example.embedchatbot.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Why: 간단/안전. 파일시스템에서 md/txt를 읽어 locale 메타와 함께 청크로 분할. */
public class MarkdownLoader {

    public List<Doc> load(String root, int maxDocChars, int chunkSize, int chunkOverlap) {
        List<Doc> docs = new ArrayList<>();
        Path base = Paths.get(root);
        if (!Files.exists(base)) return docs;

        try {
            List<Path> files = Files.walk(base)
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> {
                        String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return fn.endsWith(".md") || fn.endsWith(".txt");
                    })
                    .collect(Collectors.toList());

            int idSeq = 0;
            for (Path p : files) {
                String raw = Files.readString(p, StandardCharsets.UTF_8);
                if (raw.length() > maxDocChars) {
                    raw = raw.substring(0, maxDocChars);
                }
                String locale = guessLocale(p); // 폴더명 또는 파일명 힌트
                String title = deriveTitle(p, raw);
                String url = "file://" + base.toAbsolutePath().relativize(p.toAbsolutePath()).toString();

                List<String> chunks = chunk(raw, chunkSize, chunkOverlap);
                for (int i = 0; i < chunks.size(); i++) {
                    String id = "d" + (++idSeq);
                    String t = title + " (part " + (i + 1) + "/" + chunks.size() + ")";
                    docs.add(new Doc(id, t, url + "#part-" + (i + 1), chunks.get(i), locale));
                }
            }
        } catch (IOException e) {
            // 로깅은 상위에서
        }
        return docs;
    }

    static String guessLocale(Path p) {
        String path = p.toString().toLowerCase(Locale.ROOT);
        if (path.contains("/ko/") || path.endsWith("_ko.md") || path.endsWith("_ko.txt")) return "ko";
        if (path.contains("/ja/") || path.endsWith("_ja.md") || path.endsWith("_ja.txt")) return "ja";
        if (path.contains("/en/") || path.endsWith("_en.md") || path.endsWith("_en.txt")) return "en";
        return "ko"; // 기본 한국어
    }

    static String deriveTitle(Path p, String raw) {
        for (String line : raw.split("\n")) {
            String t = line.trim();
            if (t.startsWith("#")) return t.replaceFirst("^#+\\s*", "");
            if (!t.isEmpty()) return t.length() > 80 ? t.substring(0, 80) + "…" : t;
        }
        return p.getFileName().toString();
    }

    static List<String> chunk(String text, int size, int overlap) {
        List<String> out = new ArrayList<>();
        if (size <= 0) { out.add(text); return out; }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            out.add(text.substring(start, end));
            if (end == text.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return out;
    }
}
