#!/usr/bin/env bash
. ~/conf/metadatahub.properties
CLIENT_ID="${client_id}"
CLIENT_SECRET="${client_secret}"
BASIC_AUTH=$(printf "%s:%s" "$CLIENT_ID" "$CLIENT_SECRET" | base64 | tr -d '\n')

ACCESS_TOKEN=$(curl -s -X POST ${sso_endpoint}  \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $BASIC_AUTH" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

echo "export ACCESS_TOKEN=$ACCESS_TOKEN"



