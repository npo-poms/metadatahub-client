#!/usr/bin/env bash

. auth.sh

curl -G "${sparql_endpoint}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode 'query=
    PREFIX ec: <http://www.ebu.ch/metadata/ontologies/ebucoreplus#>
    SELECT ?title ?description ?dateCreated
    WHERE {
      ?entity ec:hasIdentifier ?id .
      ?id ec:identifierValue "VPWON_1257874" .
      ?id ec:name "PRID" .
      OPTIONAL { ?entity ec:title ?title . }
      OPTIONAL { ?entity ec:contentDescription ?description . }
      OPTIONAL { ?entity ec:hasDateCreated ?dateCreated . }
    }
    LIMIT 1'



