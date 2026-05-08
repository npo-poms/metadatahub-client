

SHELL:=/bin/bash

run:
	export CP=`mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=/dev/stdout -q`; \
	java -cp target/metadatahub-client-0.1-SNAPSHOT.jar:$$CP src/test/java/Example.java


rundocker:
	docker run -it -v .:/mnt -v ~/.m2:/root/.m2 -v ~/conf:/root/conf -w /mnt maven:3-eclipse-temurin-25-alpine \
		sh -c 'mvn -q package -DskipTests && CP=$$(mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=/dev/stdout -q) && echo $$CP && java -cp target/metadatahub-client-0.1-SNAPSHOT.jar:$$CP src/test/java/Example.java'
