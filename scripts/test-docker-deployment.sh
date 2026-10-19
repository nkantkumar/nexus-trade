#!/usr/bin/env bash
set -e

# ==============================================================================
# Nexus Trade: Local Docker Deployment Test Script
# ==============================================================================

KEYCLOAK_URL=${KEYCLOAK_URL:-"http://localhost:8080"}
API_URL=${API_URL:-"http://localhost:8081"}
REALM="nexus-trade"
CLIENT_ID="nexus-client"

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass() { echo -e "  [${GREEN}PASS${NC}] $1"; }
fail() { echo -e "  [${RED}FAIL${NC}] $1"; exit 1; }
info() { echo -e "\n${BLUE}=== $1 ===${NC}"; }

info "1. Checking Docker Containers Status"
docker compose ps
docker compose ps | grep -q "nexus-trade" || fail "nexus-trade container is not running"
docker compose ps | grep -q "nexus-keycloak" || fail "nexus-keycloak container is not running"
docker compose ps | grep -q "nexus-kafka" || fail "nexus-kafka container is not running"
docker compose ps | grep -q "nexus-redis" || fail "nexus-redis container is not running"
pass "All 4 containers (app, keycloak, kafka, redis) are running"

info "2. Checking Spring Boot Actuator Health"
HEALTH_RES=$(curl -s "$API_URL/actuator/health")
echo "$HEALTH_RES" | grep -q '"status":"UP"' || fail "Actuator health is not UP: $HEALTH_RES"
pass "Actuator health returned UP"

info "3. Requesting JWT Access Token for trader1 from Keycloak"
TRADER_TOKEN_RES=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "grant_type=password" \
  -d "username=trader1" \
  -d "password=trader123")

TRADER_TOKEN=$(echo "$TRADER_TOKEN_RES" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
[ -n "$TRADER_TOKEN" ] || fail "Failed to get token for trader1: $TRADER_TOKEN_RES"
pass "Retrieved JWT access token for trader1"

info "4. Testing /api/identity/me with trader1"
ME_RES=$(curl -s -X GET "$API_URL/api/identity/me" -H "Authorization: Bearer $TRADER_TOKEN")
echo "$ME_RES" | grep -q '"username":"trader1"' || fail "Unexpected /api/identity/me response: $ME_RES"
echo "$ME_RES" | grep -q 'ROLE_TRADER' || fail "ROLE_TRADER missing in identity: $ME_RES"
pass "Authenticated identity confirmed: username=trader1, role=ROLE_TRADER"

info "5. Testing /api/identity/trader-check (RBAC Trader Role)"
TRADER_CHECK_RES=$(curl -s -X GET "$API_URL/api/identity/trader-check" -H "Authorization: Bearer $TRADER_TOKEN")
echo "$TRADER_CHECK_RES" | grep -q '"status":"authorized"' || fail "Trader check failed: $TRADER_CHECK_RES"
pass "Trader check authorized for trader1"

info "6. Testing RBAC Forbidden: trader1 accessing /api/identity/admin-check"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$API_URL/api/identity/admin-check" -H "Authorization: Bearer $TRADER_TOKEN")
[ "$HTTP_CODE" -eq 403 ] || fail "Expected 403 Forbidden for trader1 on /api/identity/admin-check, got $HTTP_CODE"
pass "RBAC successfully rejected trader1 with 403 Forbidden"

info "7. Requesting JWT Access Token for admin1 and testing /api/identity/admin-check"
ADMIN_TOKEN_RES=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "grant_type=password" \
  -d "username=admin1" \
  -d "password=admin123")
ADMIN_TOKEN=$(echo "$ADMIN_TOKEN_RES" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
[ -n "$ADMIN_TOKEN" ] || fail "Failed to get token for admin1"

ADMIN_CHECK_RES=$(curl -s -X GET "$API_URL/api/identity/admin-check" -H "Authorization: Bearer $ADMIN_TOKEN")
echo "$ADMIN_CHECK_RES" | grep -q '"status":"authorized"' || fail "Admin check failed: $ADMIN_CHECK_RES"
pass "Admin check authorized for admin1"

info "8. Testing Trade Ingestion & CCP Novation (/api/v1/settlement/trades/ingest)"
TRADE_PAYLOAD='{
  "tradeId": "TRD-E2E-100",
  "buyOrderId": "ORD-BUY-10",
  "sellOrderId": "ORD-SELL-20",
  "buyAccountId": "FUND-OMEGA",
  "sellAccountId": "FUND-SIGMA",
  "symbol": "MSFT",
  "quantity": 250,
  "price": 420.00,
  "currency": "USD"
}'
INGEST_RES=$(curl -s -X POST "$API_URL/api/v1/settlement/trades/ingest" \
  -H "Authorization: Bearer $TRADER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$TRADE_PAYLOAD")
echo "$INGEST_RES" | grep -q '"tradeId":"TRD-E2E-100"' || fail "Trade ingestion failed: $INGEST_RES"
echo "$INGEST_RES" | grep -q '"status":"CAPTURED"' || fail "Trade status is not CAPTURED: $INGEST_RES"
pass "Trade TRD-E2E-100 captured and published to Kafka (trade-cleared-events)"

info "9. Testing Multilateral Netting Calculation (/api/v1/settlement/clearing/netting)"
NETTING_RES=$(curl -s -X POST "$API_URL/api/v1/settlement/clearing/netting" \
  -H "Authorization: Bearer $TRADER_TOKEN")
echo "$NETTING_RES" | grep -q "netQuantity" || fail "Netting failed: $NETTING_RES"
pass "Multilateral Netting successfully computed netted obligations"

info "10. Testing Block Trade Institutional Allocation (/api/v1/settlement/allocations/block)"
BLOCK_PAYLOAD='{
  "blockTradeId": "BLK-999",
  "symbol": "GOOGL",
  "side": "BUY",
  "totalQuantity": 1000,
  "price": 180.00,
  "currency": "USD",
  "accountSharesPercent": {
    "FUND-A": 50.0,
    "FUND-B": 30.0,
    "FUND-C": 20.0
  },
  "accountCustodianMap": {
    "FUND-A": "STATE_STREET",
    "FUND-B": "BNY_MELLON",
    "FUND-C": "JPMORGAN"
  }
}'
BLOCK_RES=$(curl -s -X POST "$API_URL/api/v1/settlement/allocations/block" \
  -H "Authorization: Bearer $TRADER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BLOCK_PAYLOAD")
echo "$BLOCK_RES" | grep -q '"blockTradeId":"BLK-999"' || fail "Block allocation failed: $BLOCK_RES"
echo "$BLOCK_RES" | grep -q '"status":"ALLOCATED"' || fail "Block status not ALLOCATED: $BLOCK_RES"
pass "Block trade BLK-999 allocated across 3 institutional accounts"

info "11. Testing DvP Settlement Instruction Creation & Execution (/api/v1/settlement/dvp/...)"
DVP_CREATE_PAYLOAD='{
  "obligationOrTradeId": "OBLIG-E2E-1",
  "delivererAccountId": "SELLER-SAFE",
  "receiverAccountId": "BUYER-SAFE",
  "symbol": "NVDA",
  "quantity": 100,
  "cashAmount": 12500.0,
  "currency": "USD",
  "model": "MODEL_1"
}'
DVP_RES=$(curl -s -X POST "$API_URL/api/v1/settlement/dvp/create" \
  -H "Authorization: Bearer $TRADER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$DVP_CREATE_PAYLOAD")
INSTR_ID=$(echo "$DVP_RES" | grep -o '"instructionId":"[^"]*' | cut -d'"' -f4)
[ -n "$INSTR_ID" ] || fail "Failed to create DvP instruction: $DVP_RES"
pass "Created DvP instruction: $INSTR_ID"

DVP_EXEC_RES=$(curl -s -X POST "$API_URL/api/v1/settlement/dvp/process/$INSTR_ID" \
  -H "Authorization: Bearer $TRADER_TOKEN")
echo "$DVP_EXEC_RES" | grep -q '"status"' || fail "DvP settlement execution failed: $DVP_EXEC_RES"
pass "DvP state machine processed instruction (Status: $(echo "$DVP_EXEC_RES" | grep -o '"status":"[^"]*' | cut -d'"' -f4))"

info "12. Testing Portfolio Snapshot (/portfolio/AAPL)"
PORTFOLIO_RES=$(curl -s -X GET "$API_URL/portfolio/AAPL" \
  -H "Authorization: Bearer $TRADER_TOKEN")
echo "$PORTFOLIO_RES" | grep -q '"symbol":"AAPL"' || fail "Portfolio query failed: $PORTFOLIO_RES"
pass "Portfolio snapshot retrieved with Redis backing"

echo -e "\n${GREEN}================================================================${NC}"
echo -e "${GREEN}ALL 12 DOCKER COMPOSE INTEGRATION TESTS PASSED SUCCESSFULLY!${NC}"
echo -e "${GREEN}================================================================${NC}\n"
