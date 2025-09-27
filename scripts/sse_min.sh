# File: scripts/sse_min.sh  (최소 출력: 도중 파싱 없이 원본 그대로 보기)
#!/usr/bin/env bash
set -euo pipefail
API="${1:-http://localhost:9000}"
KEY="${CHAT_API_KEY:-dev-key}"
BOT="${BOT_ID:-sample-bot}"
MSG="${2:-hello sse}"
curl -S -s -N \
  -H "Accept: text/event-stream" \
  -H "X-API-Key: $KEY" \
  --get \
  --data-urlencode "botId=$BOT" \
  --data-urlencode "message=$MSG" \
  "$API/v1/chat/stream"
