#!/bin/bash

docker stop escluster && docker rm escluster
docker stop izagent && docker rm izagent
docker stop neo && docker rm -v neo
rm -r $HOME/neo4j/data
docker stop informiz && docker rm informiz
docker stop lands_ser && docker rm lands_ser
docker stop iz-rabbit && docker rm iz-rabbit
docker network rm informiz_net


