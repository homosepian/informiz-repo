FROM java:8
MAINTAINER amitnira@gmail.com
CMD { echo "java -jar lands-service-1.0-SNAPSHOT.jar"; (paste -d, -s neo4j.properties | sed 's/neo4j.user=\([^,]*\),neo4j.pass=\([^,]*\),neo4j.host=\([^,]*\),/"\1" "\2" "\3"/'); } | tr "\n" " " | sh
ADD build/lands-service-1.0-SNAPSHOT.jar .
# This is a security breach, see https://github.com/docker/docker/issues/13490
ADD neo4j.properties .

