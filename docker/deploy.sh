#!/bin/bash

# build images
docker build -t flume -f $(pwd)/flume/Dockerfile $(pwd)/flume
docker build -t elasticsearch -f $(pwd)/elasticsearch/Dockerfile $(pwd)/elasticsearch
docker build -t lands_ser -f $(pwd)/lands-ser/Dockerfile $(pwd)/lands-ser
docker build -t lands_rest -f $(pwd)/lands-rest/Dockerfile $(pwd)/lands-rest

# create a bridge network for the app
docker network create informiz_net
# for multiple hosts: docker network create -d overlay informiz_net

# start elasticsearch
docker run -d --memory-swappiness=0 --net=informiz_net --name=escluster -p 9300:9300 -p 9200:9200 elasticsearch
#start flume
docker run -d --net=informiz_net --name=izagent -p 44444:44444 -t flume
#start landscape service on host, where neo4j is running. TODO: run neo4j in container
docker run -d --net=host --name=lands_ser lands_ser
#start landscape endpoint
docker run -d --net=host --name=lands_rest -p 8080:8080 lands_rest
