# apply_streaming_only.sh — 리포 루트에서 실행
# 사용법:
#   bash apply_streaming_only.sh
#   ./mvnw -q -DskipTests compile && ./mvnw spring-boot:run
set -euo pipefail

ROOT="$(pwd)"

# 0) 경로 존재 확인
test -f "$ROOT/pom.xml" || { echo "여기가 리포 루트가 아닙니다(pom.xml 없음)"; exit 1; }

mkdir -p src/main/java/com/example/embedchatbot/service
mkdir -p src/main/java/com/example/embedchatbot/controller
mkdir -p src/main/resources
mkdir -p frontend

# 1) ChatStreamService.java 교체
cat > src/main/java/com/example/embedchatbot/service/ChatStreamService.java <<'EOF'
package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ChatStreamService {

    public interface Sink {
        void onTokenJson(String jsonToken);   // {"t":"…"}
        void onUsageJson(String jsonUsage);   // {"promptTokens":..,"completionTokens":..}
        void onDone();
        void onError(String message);
        void onHeartbeat();                   // SSE keep-alive (":\n")
    }

    private final ChatService chatService;
    private final long delayMs;
    private final long heartbeatMs;

    public ChatStreamService(
            ChatService chatService,
            @Value("${app.stream.delay-ms:8}") long delayMs,
            @Value("${app.stream.heartbeat-ms:15000}") long heartbeatMs
    ) {
        this.chatService = chatService;
        this.delayMs = Math.max(0, delayMs);
        this.heartbeatMs = Math.max(1000, heartbeatMs);
    }

    public void stream(String botId, String message, String sessionId, Sink sink) {
        CompletableFuture.runAsync(() -> {
            try {
                final ChatRequest req = buildRequest(botId, message, sessionId);
                final ChatResult result = chatService.chat(req);

                String reply = extractAnswer(result);
                if (reply == null) reply = "";
                reply = reply.replace("\r\n", "\n");

                long lastBeat = System.currentTimeMillis();
                for (int i = 0; i < reply.length(); ) {
                    int cp = reply.codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    sink.onTokenJson("{\"t\":\"" + jsonEsc(ch) + "\"}");
                    i += Character.charCount(cp);

                    if (delayMs > 0) Thread.sleep(delayMs);
                    long now = System.currentTimeMillis();
                    if (now - lastBeat >= heartbeatMs) {
                        sink.onHeartbeat();  // ":\n"
                        lastBeat = now;
                    }
                }

                Object usage = extractUsage(result);
                if (usage != null) {
                    long pt = safeTokens(usage, true);
                    long ct = safeTokens(usage, false);
                    sink.onUsageJson("{\"promptTokens\":" + Math.max(0, pt) + ",\"completionTokens\":" + Math.max(0, ct) + "}");
                }
                sink.onDone();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                sink.onError("interrupted");
            } catch (Throwable t) {
                sink.onError(t.getMessage() == null ? "error" : t.getMessage());
            }
        });
    }

    // ---- helpers (record/POJO 호환; 리플렉션) ----
    private static ChatRequest buildRequest(String botId, String message, String sessionId) {
        try {
            return ChatRequest.class
                    .getDeclaredConstructor(String.class, String.class, String.class)
                    .newInstance(botId, message, sessionId);
        } catch (NoSuchMethodException e) {
            try {
                ChatRequest req = ChatRequest.class.getDeclaredConstructor().newInstance();
                ChatRequest.class.getMethod("setBotId", String.class).invoke(req, botId);
                ChatRequest.class.getMethod("setMessage", String.class).invoke(req, message);
                ChatRequest.class.getMethod("setSessionId", String.class).invoke(req, sessionId);
                return req;
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot construct ChatRequest", ex);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot construct ChatRequest", e);
        }
    }

    private static String extractAnswer(ChatResult r) {
        if (r == null) return "";
        try { return (String) r.getClass().getMethod("answer").invoke(r); }
        catch (NoSuchMethodException ignore) {
            try { return (String) r.getClass().getMethod("getAnswer").invoke(r); }
            catch (Exception e) { return ""; }
        } catch (Exception e) { return ""; }
    }
    private static Object extractUsage(ChatResult r) {
        if (r == null) return null;
        try { return r.getClass().getMethod("usage").invoke(r); }
        catch (NoSuchMethodException ignore) {
            try { return r.getClass().getMethod("getUsage").invoke(r); }
            catch (Exception e) { return null; }
        } catch (Exception e) { return null; }
    }
    private static long safeTokens(Object usage, boolean prompt) {
        try {
            if (prompt) {
                try { return ((Number) usage.getClass().getMethod("promptTokens").invoke(usage)).longValue(); }
                catch (NoSuchMethodException ignore) { return ((Number) usage.getClass().getMethod("getPromptTokens").invoke(usage)).longValue(); }
            } else {
                try { return ((Number) usage.getClass().getMethod("completionTokens").invoke(usage)).longValue(); }
                catch (NoSuchMethodException ignore) { return ((Number) usage.getClass().getMethod("getCompletionTokens").invoke(usage)).longValue(); }
            }
        } catch (Exception e) { return 0L; }
    }

    private static String jsonEsc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> b.append("\\\\");
                case '\"' -> b.append("\\\"");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
EOF

# 2) ChatStreamController.java 교체
cat > src/main/java/com/example/embedchatbot/controller/ChatStreamController.java <<'EOF'
package com.example.embedchatbot.controller;

import com.example.embedchatbot.service.ChatStreamService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
@Validated
public class ChatStreamController {

    private final ChatStreamService streamService;

    public ChatStreamController(ChatStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam @NotBlank @Size(max = 64) String botId,
            @RequestParam @NotBlank @Size(min = 1, max = 4000) String message,
            @RequestParam(required = false) @Size(max = 128) String sessionId
    ) {
        final SseEmitter emitter = new SseEmitter(0L);
        streamService.stream(botId, message, sessionId, new ChatStreamService.Sink() {
            @Override public void onTokenJson(String j) {
                try { emitter.send(SseEmitter.event().name("token").data(j)); }
                catch (Exception e) { emitter.completeWithError(e); }
            }
            @Override public void onUsageJson(String j) {
                try { emitter.send(SseEmitter.event().name("usage").data(j)); }
                catch (Exception e) { emitter.completeWithError(e); }
            }
            @Override public void onDone() {
                try { emitter.send(SseEmitter.event().name("done").data("[DONE]")); } catch (Exception ignore) {}
                emitter.complete();
            }
            @Override public void onError(String m) {
                try { emitter.send(SseEmitter.event().name("error").data("{\"error\":\""+m+"\"}")); } catch (Exception ignore) {}
                emitter.completeWithError(new RuntimeException(m));
            }
            @Override public void onHeartbeat() {
                try { emitter.send(":"); } catch (Exception ignore) {}
            }
        });
        return emitter;
    }
}
EOF

# 3) application.yml 업데이트(키 보존 + stream 설정 추가)
if ! grep -q "app:" src/main/resources/application.yml; then
  echo "server:
  port: 9000
app:
  cors:
    allowed-origins: \${ALLOWED_ORIGINS:http://localhost:8000,http://127.0.0.1:8000}
  stream:
    delay-ms: \${STREAM_DELAY_MS:8}
    heartbeat-ms: \${STREAM_HEARTBEAT_MS:15000}
" > src/main/resources/application.yml
else
  # idempotent merge: stream 섹션이 없으면 추가
  if ! grep -q "stream:" src/main/resources/application.yml; then
    cat >> src/main/resources/application.yml <<'EOF'

app:
  stream:
    delay-ms: ${STREAM_DELAY_MS:8}
    heartbeat-ms: ${STREAM_HEARTBEAT_MS:15000}
EOF
  else
    # ensure keys exist
    grep -q "delay-ms" src/main/resources/application.yml || \
      sed -i '' -e '/stream:/a\
\ \ \ \ delay-ms: ${STREAM_DELAY_MS:8}
' src/main/resources/application.yml || true
    grep -q "heartbeat-ms" src/main/resources/application.yml || \
      sed -i '' -e '/stream:/a\
\ \ \ \ heartbeat-ms: ${STREAM_HEARTBEAT_MS:15000}
' src/main/resources/application.yml || true
  fi
fi

# 4) 프론트 스트리밍 전용 chat.html 교체
cat > frontend/chat.html <<'EOF'
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <title>Embed Chatbot — Streaming Only</title>
  <style>
    body{font-family:system-ui;margin:0;background:#f6f7f9}
    .wrap{max-width:720px;margin:32px auto;padding:0 16px}
    .card{background:#fff;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.06);overflow:hidden}
    .head{padding:16px;font-weight:700;border-bottom:1px solid #eee}
    .msgs{height:420px;overflow:auto;padding:16px;display:flex;flex-direction:column;gap:12px}
    .msg{padding:10px 12px;border-radius:14px;max-width:80%}
    .me{align-self:flex-end;background:#e9f2ff;white-space:pre-wrap}
    .bot{align-self:flex-start;background:#f1f3f5;white-space:pre-wrap}
    .foot{display:grid;grid-template-columns:1fr auto;gap:8px;padding:12px;border-top:1px solid #eee}
    button{border:0;border-radius:10px;padding:10px 14px;cursor:pointer}
    .error{color:#b00020;font-size:12px;padding:0 12px 12px}
  </style>
</head>
<body>
<div class="wrap">
  <div class="card" role="region" aria-label="Chatbot">
    <div class="head">Embed Chatbot — Streaming Only</div>
    <div id="msgs" class="msgs" aria-live="polite"></div>
    <div class="error" id="err" aria-live="assertive"></div>
    <div class="foot">
      <input id="input" placeholder="메시지를 입력하세요" aria-label="message"/>
      <button id="send">전송</button>
    </div>
  </div>
</div>
<script>
  const API = (window.CHAT_API_BASE || "http://localhost:9000");
  const BOT = (window.CHAT_BOT_ID || "sample-bot");
  const SID = (() => { const k="chat_session_id"; const v=localStorage.getItem(k)||String(Date.now()); localStorage.setItem(k,v); return v; })();

  const $msgs=document.getElementById('msgs');
  const $err=document.getElementById('err');
  const $in=document.getElementById('input');
  const $btn=document.getElementById('send');

  function push(role, text) {
    const div = document.createElement('div');
    div.className = 'msg ' + (role === 'user' ? 'me' : 'bot');
    div.textContent = text;
    $msgs.appendChild(div);
    $msgs.scrollTop = $msgs.scrollHeight;
    return div;
  }

  function sendStream() {
    $err.textContent = '';
    const text = ($in.value||'').trim(); if(!text) return;
    push('user', text);
    $in.value='';
    const botMsg = push('assistant', '');

    const url = API + '/v1/chat/stream'
      + '?botId=' + encodeURIComponent(BOT)
      + '&sessionId=' + encodeURIComponent(SID)
      + '&message=' + encodeURIComponent(text);

    const es = new EventSource(url);
    es.addEventListener('token', (ev) => {
      try {
        const { t } = JSON.parse(ev.data);
        botMsg.textContent += (t ?? "");
      } catch { botMsg.textContent += ev.data; } // 구 형식 호환
      $msgs.scrollTop = $msgs.scrollHeight;
    });
    es.addEventListener('usage', () => {});
    es.addEventListener('done', () => { es.close(); });
    es.onerror = async () => {
      es.close();
      try {
        const r = await fetch(url, { headers: { 'Accept': 'text/event-stream' } });
        const txt = await r.text();
        $err.textContent = `스트림 오류: HTTP ${r.status} ${r.statusText} — ${txt.slice(0,200)}`;
      } catch (e) {
        $err.textContent = '스트림 오류: ' + (e.message||e);
      }
    };
  }

  $btn.addEventListener('click', sendStream);
  $in.addEventListener('keydown', (e) => { if(e.key==='Enter' && !e.shiftKey) sendStream(); });
</script>
</body>
</html>
EOF

# 5) 단발 컨트롤러가 있다면 삭제(선택)
if test -f src/main/java/com/example/embedchatbot/controller/ChatController.java; then
  git rm -f src/main/java/com/example/embedchatbot/controller/ChatController.java || true
fi

echo "==> Done. 이제 빌드/실행하세요."
