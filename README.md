- mvn spring-boot:run (기본 포트가 9000이 되도록 위 설정 반영)
- 로컬 개발 시 http://localhost:8000, http://127.0.0.1:8000 도메인에서 CORS 허용

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
