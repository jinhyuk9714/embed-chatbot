# File: scripts/sse_smoke_gawk.sh  (옵션) — gawk 설치 시 사용 가능
#!/usr/bin/env bash
set -euo pipefail
API="${1:-http://localhost:9000}"
KEY="${CHAT_API_KEY:-dev-key}"
BOT="${BOT_ID:-sample-bot}"
MSG="${2:-hello sse}"

echo ">>> Hitting $API/v1/chat/stream (gawk)"
curl -S -s -N \
  -H "Accept: text/event-stream" \
  -H "X-API-Key: $KEY" \
  --get \
  --data-urlencode "botId=$BOT" \
  --data-urlencode "message=$MSG" \
  "$API/v1/chat/stream" \
| gawk '
  function ts(){ return strftime("[%H:%M:%S]") }
  /^event:/ { ev=$0; next }
  /^data:/  { print ts(), ev, $0; next }
  /^:$/     { print ts(), "event: keepalive"; next }'
