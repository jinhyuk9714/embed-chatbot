- mvn spring-boot:run (기본 포트가 9000이 되도록 위 설정 반영)
- 로컬 개발 시 http://localhost:8000, http://127.0.0.1:8000 도메인에서 CORS 허용

### 프론트엔드 페이지 실행

```bash
cd frontend
python -m http.server 8000
```

브라우저에서 http://localhost:8000/chat.html 에 접속하면 됩니다.

### API 테스트 예시

```
curl -X POST "http://localhost:9000/v1/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "botId": "sample-bot",
    "message": "Hello there!",
    "sessionId": "demo-session"
  }'
```
