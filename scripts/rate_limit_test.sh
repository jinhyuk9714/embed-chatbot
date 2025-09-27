set -euo pipefail
API="${1:-http://localhost:9000}"
KEY="${CHAT_API_KEY:-dev-key}"
BOT="sample-bot"
echo ">>> Firing many parallel requests to trigger 429"
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Accept: text/event-stream" -H "X-API-Key: $KEY" \
    --get --data-urlencode "botId=$BOT" \
    --data-urlencode "message=hit-$i" \
    "$API/v1/chat/stream" &
done
wait
echo ">>> Check server logs for 429 and X-RateLimit headers"