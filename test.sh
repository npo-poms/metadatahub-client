#!/usr/bin/env bash
 BASIC_AUTH=$(printf "%s:%s" "$CLIENT_ID" "$CLIENT_SECRET" | base64 | tr -d '\n')




curl -X POST https://sso.metadatahub.bijnpo.nl/auth/realms/MDS-ACC/protocol/openid-connect/token  \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $BASIC_AUTH" \
  -d "grant_type=client_credentials"



