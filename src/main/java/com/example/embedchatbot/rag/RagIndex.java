// =================================================
// File: server/src/main/java/com/example/embedchatbot/rag/RagIndex.java
// =================================================
package com.example.embedchatbot.rag;

import java.util.*;

/** Why: 외부 라이브러리 없이 TF-IDF/코사인. 작은 코퍼스에 충분. */
public class RagIndex {
    private final List<Doc> docs;
    private final Map<String,Integer> vocab = new HashMap<>();
    private final double[][] tfidf; // [doc][term]
    private final double[] norms;

    public RagIndex(List<Doc> docs) {
        this.docs = docs;
        // vocab
        for (Doc d : docs) for (String tok : tokenize(d.text)) vocab.putIfAbsent(tok, vocab.size());
        int V = vocab.size();
        int N = docs.size();

        // DF
        int[] df = new int[V];
        for (Doc d : docs) {
            boolean[] seen = new boolean[V];
            for (String tok : tokenize(d.text)) {
                Integer idx = vocab.get(tok);
                if (idx != null && !seen[idx]) { df[idx]++; seen[idx] = true; }
            }
        }
        double[] idf = new double[V];
        for (int i = 0; i < V; i++) idf[i] = Math.log((N + 1.0) / (df[i] + 1.0)) + 1.0;

        // TF-IDF
        tfidf = new double[N][V];
        for (int i = 0; i < N; i++) {
            Map<Integer,Integer> tf = new HashMap<>();
            for (String tok : tokenize(docs.get(i).text)) {
                Integer idx = vocab.get(tok); if (idx != null) tf.merge(idx, 1, Integer::sum);
            }
            for (Map.Entry<Integer,Integer> e : tf.entrySet()) {
                tfidf[i][e.getKey()] = e.getValue() * idf[e.getKey()];
            }
        }

        // norms
        norms = new double[N];
        for (int i = 0; i < N; i++) {
            double s = 0; for (double v : tfidf[i]) s += v*v;
            norms[i] = Math.sqrt(s);
        }
    }

    public List<Snippet> retrieve(String query, String locale, int k) {
        if (docs.isEmpty()) return List.of();
        double[] q = new double[vocab.size()];
        for (String tok : tokenize(query)) {
            Integer idx = vocab.get(tok);
            if (idx != null) q[idx] += 1.0;
        }
        // optional: locale 가중치
        double qnorm = l2(q);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));
        for (int i = 0; i < docs.size(); i++) {
            double s = cosine(q, qnorm, tfidf[i], norms[i]);
            if (locale != null && !locale.isBlank() && !locale.equalsIgnoreCase(docs.get(i).locale)) {
                s *= 0.85; // 다른 로캘은 살짝 패널티
            }
            if (s <= 0) continue;
            long bits = Double.doubleToRawLongBits(s);
            if (pq.size() < k) pq.add(new int[]{i, (int)(bits)}); // store as int[] to avoid custom class
            else if (Double.longBitsToDouble(pq.peek()[1]) < s) { pq.poll(); pq.add(new int[]{i,(int)(bits)}); }
        }
        List<Snippet> out = new ArrayList<>();
        while (!pq.isEmpty()) {
            int[] it = pq.poll();
            int idx = it[0];
            double score = Double.longBitsToDouble((long) it[1]);
            Doc d = docs.get(idx);
            out.add(new Snippet(d.title, d.url, d.text, score));
        }
        Collections.reverse(out);
        return out;
    }

    static double l2(double[] v) { double s=0; for (double x: v) s+=x*x; return Math.sqrt(s); }
    static double cosine(double[] a, double an, double[] b, double bn) {
        if (an==0 || bn==0) return 0;
        double dot=0; for (int i=0;i<a.length;i++) dot+=a[i]*b[i];
        return dot/(an*bn);
    }

    static List<String> tokenize(String s) {
        String norm = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣\\s]", " ");
        String[] parts = norm.split("\\s+");
        List<String> out = new ArrayList<>(parts.length);
        for (String t : parts) if (t.length() > 1) out.add(t);
        return out;
    }
}
