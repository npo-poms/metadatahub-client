#!/usr/bin/env bash

source auth.sh

Q="$(<src/main/resources/sparql/mediaobject.sparql)"
#Q="$(<src/main/resources/sparql/firstquery.sparql)"
printf -v QUERY '%s\nLIMIT 2' "$Q"
echo EXECUTING SPARQL QUERY:
echo "$QUERY"
curl -G "${sparql_endpoint}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "query=${QUERY}"



