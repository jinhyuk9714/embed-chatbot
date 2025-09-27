package com.example.embedchatbot.rag;

import java.util.*;

/**
 * Small TF-IDF index optimised for a lightweight RAG demo.
 */
public class RagIndex {
    private final List<Doc> docs;
    private final Map<String, Integer> vocab = new HashMap<>();
    private final double[][] tfidf;
    private final double[] norms;

    public RagIndex(List<Doc> docs) {
        this.docs = List.copyOf(docs);
        for (Doc doc : docs) {
            for (String token : tokenize(doc.text)) {
                vocab.putIfAbsent(token, vocab.size());
            }
        }
        int vocabSize = vocab.size();
        int docCount = docs.size();

        int[] documentFrequency = new int[vocabSize];
        for (Doc doc : docs) {
            boolean[] seen = new boolean[vocabSize];
            for (String token : tokenize(doc.text)) {
                Integer idx = vocab.get(token);
                if (idx != null && !seen[idx]) {
                    documentFrequency[idx]++;
                    seen[idx] = true;
                }
            }
        }
        double[] idf = new double[vocabSize];
        for (int i = 0; i < vocabSize; i++) {
            idf[i] = Math.log((docCount + 1.0) / (documentFrequency[i] + 1.0)) + 1.0;
        }

        tfidf = new double[docCount][vocabSize];
        for (int d = 0; d < docCount; d++) {
            Map<Integer, Integer> termFreq = new HashMap<>();
            for (String token : tokenize(docs.get(d).text)) {
                Integer idx = vocab.get(token);
                if (idx != null) {
                    termFreq.merge(idx, 1, Integer::sum);
                }
            }
            for (Map.Entry<Integer, Integer> entry : termFreq.entrySet()) {
                int idx = entry.getKey();
                tfidf[d][idx] = entry.getValue() * idf[idx];
            }
        }

        norms = new double[docCount];
        for (int d = 0; d < docCount; d++) {
            norms[d] = l2(tfidf[d]);
        }
    }

    public List<Snippet> retrieve(String query, String locale, int k) {
        if (docs.isEmpty()) {
            return List.of();
        }
        double[] queryVector = new double[vocab.size()];
        for (String token : tokenize(query)) {
            Integer idx = vocab.get(token);
            if (idx != null) {
                queryVector[idx] += 1.0;
            }
        }
        double queryNorm = l2(queryVector);

        PriorityQueue<Match> heap = new PriorityQueue<>(Comparator.comparingDouble(m -> m.score));
        for (int i = 0; i < docs.size(); i++) {
            double score = cosine(queryVector, queryNorm, tfidf[i], norms[i]);
            if (score <= 0) {
                continue;
            }
            if (locale != null && !locale.isBlank() && !locale.equalsIgnoreCase(docs.get(i).locale)) {
                score *= 0.85;
            }
            if (heap.size() < k) {
                heap.add(new Match(i, score));
            } else if (heap.peek().score < score) {
                heap.poll();
                heap.add(new Match(i, score));
            }
        }

        List<Snippet> results = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            Match match = heap.poll();
            Doc doc = docs.get(match.index);
            results.add(new Snippet(doc.title, doc.url, doc.text, match.score));
        }
        Collections.reverse(results);
        return results;
    }

    private record Match(int index, double score) {}

    static double l2(double[] vector) {
        double sum = 0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    static double cosine(double[] a, double normA, double[] b, double normB) {
        if (normA == 0 || normB == 0) {
            return 0;
        }
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot / (normA * normB);
    }

    static List<String> tokenize(String text) {
        String normalised = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣\\s]", " ");
        String[] parts = normalised.split("\\s+");
        List<String> out = new ArrayList<>(parts.length);
        for (String token : parts) {
            if (token.length() > 1) {
                out.add(token);
            }
        }
        return out;
    }
}
