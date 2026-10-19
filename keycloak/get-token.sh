#!/bin/bash
# Helper script to fetch a JWT access token from local Keycloak and test Nexus Trade endpoints

KEYCLOAK_URL=${KEYCLOAK_URL:-"http://localhost:8080"}
REALM="nexus-trade"
CLIENT_ID="nexus-client"
USERNAME=${1:-"trader1"}
PASSWORD=${2:-"trader123"}
API_URL=${API_URL:-"http://localhost:8081"}

echo "Requesting access token from Keycloak ($KEYCLOAK_URL) for user: $USERNAME..."

RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "grant_type=password" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD")

ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Failed to obtain access token. Response from Keycloak:"
  echo "$RESPONSE"
  exit 1
fi

echo "Access token successfully retrieved!"
echo ""
echo "=== Calling GET $API_URL/api/identity/me ==="
curl -s -X GET "$API_URL/api/identity/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq . 2>/dev/null || curl -s -X GET "$API_URL/api/identity/me" -H "Authorization: Bearer $ACCESS_TOKEN"
echo ""

echo "=== Calling GET $API_URL/api/identity/trader-check ==="
curl -s -X GET "$API_URL/api/identity/trader-check" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq . 2>/dev/null || curl -s -X GET "$API_URL/api/identity/trader-check" -H "Authorization: Bearer $ACCESS_TOKEN"
echo ""
