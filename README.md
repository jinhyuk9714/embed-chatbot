# embed-chatbot

외부 웹페이지에 붙여 테스트할 수 있는 임베드형 챗봇 데모입니다. Spring Boot 서버가 SSE(Server-Sent Events) 스트림으로 챗봇 응답을 전달하고, `frontend/chat.html`에서 로컬 테스트 UI를 제공합니다.

## 주요 기능

- `GET /v1/chat/stream` 엔드포인트로 토큰 스트리밍 응답 제공
- OpenAI Chat Completions API 연동용 WebClient 구성
- `OPENAI_API_KEY`가 없을 때 에코 응답으로 동작하는 fallback 처리
- `data/docs` 아래의 Markdown 또는 TXT 문서를 읽는 간단한 RAG 검색 흐름
- IP와 세션 기준 rate limit 필터
- 로컬 테스트용 HTML 프론트엔드와 SSE 확인 스크립트
- OpenAPI 문서 파일 제공

## 기술 스택

- Java 17
- Spring Boot 3.5.6
- Spring Web MVC, WebFlux WebClient
- Bucket4j
- Maven Wrapper
- HTML, JavaScript

## 프로젝트 구조

```text
src/main/java/com/example/embedchatbot/
├── chat/      # SSE 컨트롤러와 채팅 스트림 서비스
├── config/    # CORS, OpenAI WebClient, RAG 설정
├── llm/       # OpenAI Chat Completions 호출 클라이언트
├── rag/       # 문서 로딩과 검색 흐름
└── rate/      # IP/세션 rate limit 필터

frontend/
├── chat.html
├── demo.html
└── test-sse.html

scripts/
└── *.sh       # SSE, rate limit, 환경 변수 확인용 스크립트
```

## 실행 방법

서버 기본 포트는 `9000`입니다.

```bash
./mvnw spring-boot:run
```

실제 OpenAI 응답을 사용하려면 실행 전에 `OPENAI_API_KEY` 환경 변수를 설정합니다. 키가 없으면 서버는 입력 메시지를 에코하는 방식으로 응답합니다.

프론트엔드 테스트 페이지는 별도 정적 서버로 실행합니다.

```bash
cd frontend
python3 -m http.server 8000
```

브라우저에서 `http://localhost:8000/chat.html`을 열고 API 주소를 `http://localhost:9000`으로 둔 뒤 메시지를 전송합니다. 기본 CORS 설정은 `http://localhost:8000`과 `http://127.0.0.1:8000`을 허용합니다.

## API 예시

```bash
curl -N "http://localhost:9000/v1/chat/stream?message=hello&sessionId=demo-session"
```

SSE 이벤트는 `token`, `usage`, `keepalive`, `done` 이름으로 전달됩니다.

## 테스트

단위 테스트는 Maven Wrapper로 실행합니다.

```bash
./mvnw test
```

통합 테스트는 기본 설정에서 건너뛰도록 되어 있으며, 필요할 때 다음처럼 실행할 수 있습니다.

```bash
./mvnw verify -DskipITs=false
```

## 설정 메모

- `OPENAI_MODEL` 기본값은 `gpt-4o-mini`입니다.
- `OPENAI_BASE_URL` 기본값은 `https://api.openai.com/v1`입니다.
- RAG 문서 경로 기본값은 `data/docs`입니다.
- rate limit 값은 `RL_IP_*`, `RL_SESS_*` 환경 변수로 조정할 수 있습니다.
