#!/usr/bin/env bash

. auth.sh

curl -G "${sparql_endpoint}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode 'query=
    PREFIX ec: <http://www.ebu.ch/metadata/ontologies/ebucoreplus#>
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    SELECT ?prid ?type
           ?entity
          ?dateCreated ?dateModified
          ?title ?description
          ?channelName ?start ?end
          ?genreLabel ?targetAudienceLabel ?ratingType ?ratingValue
    WHERE {
      ?entity ec:hasIdentifier ?id .
      ?id ec:name "PRID" .
      ?entity ec:hasPublication ?publication .
        ?publication ec:hasPublicationChannel ?channel .
        ?channel ec:name ?channelName .
        ?publication ec:hasStartDateTime ?start .
      OPTIONAL { ?entity ec:title ?title . }
      OPTIONAL { ?entity ec:contentDescription ?description . }
      OPTIONAL { ?entity ec:hasDateCreated ?dateCreated . }
      OPTIONAL {
          ?entity ec:hasGenre ?genre .
          ?genre skos:prefLabel ?genreLabel .
        }
        OPTIONAL {
          ?entity ec:hasTargetAudience ?audience .
          ?audience ec:hasObjectType/skos:prefLabel ?targetAudienceLabel .
        }
        OPTIONAL {
          ?entity ec:hasRating ?rating .
          ?rating a ?ratingType .
          ?rating ec:ratingValue ?ratingValue .
        }
    }
    LIMIT 10'



