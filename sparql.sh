#!/usr/bin/env bash

source auth.sh

#Q="$(<sparql/query_by_day_and_channel.sparql)"
#Q="$(<sparql/mediaobject.sparql)"

Q=$(<$1)
#Q="$(<src/main/resources/sparql/firstquery.sparql)"
printf -v QUERY '%s\nLIMIT 2' "$Q"
#echo EXECUTING SPARQL QUERY:
#echo "$QUERY"
curl -G "${sparql_endpoint}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "query=${QUERY}"



