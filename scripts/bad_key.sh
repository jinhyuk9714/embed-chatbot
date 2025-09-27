set -euo pipefail
API="${1:-http://localhost:9000}"
echo ">>> Expect 401"
curl -i -H "Accept: text/event-stream" \
     -H "X-API-Key: WRONG" \
     --get --data-urlencode "botId=sample" \
     --data-urlencode "message=hi" \
     "$API/v1/chat/stream" | sed -n '1,20p'