#!/bin/bash

# TODO: auth 
# TODO: only publish web port

# build images
docker build -t flume -f $(pwd)/flume/Dockerfile $(pwd)/flume
docker build -t elasticsearch -f $(pwd)/elasticsearch/Dockerfile $(pwd)/elasticsearch
docker build -t lands_ser -f $(pwd)/lands-ser/Dockerfile $(pwd)/lands-ser
docker build -t lands_rest -f $(pwd)/lands-rest/Dockerfile $(pwd)/lands-rest
docker build -t iz_graph -f $(pwd)/iz-graph/Dockerfile $(pwd)/iz-graph
docker build -t iz_search -f $(pwd)/iz-search/Dockerfile $(pwd)/iz-search

# create a bridge network for the app
docker network create informiz_net
# for multiple hosts: docker network create -d overlay informiz_net

# start neo4j 
docker run -d --net=informiz_net --name=neo --volume=$HOME/neo4j/data:/data --volume=$(pwd)/iz-graph/resources:/resources --env=NEO4J_AUTH=none -p=7474:7474 neo4j
# start elasticsearch
docker run -d --memory-swappiness=0 --net=informiz_net --name=escluster -p 9300:9300 -p 9200:9200 elasticsearch
# start flume
docker run -d --net=informiz_net --name=izagent -p 44444:44444 -t flume
# start rabbitmq
docker run -d --net=informiz_net --hostname=iz-rabbit --name=iz-rabbit rabbitmq:latest

sleep 10

# load data to graph
docker run --rm --net=informiz_net --name=iz_graph --volume=$(pwd)/conf:/conf --volume=$(pwd)/iz-graph/resources:/resources iz_graph

# export data from neo4j to elasticsearch
docker run --rm --net=informiz_net --name=iz_search --volume=$(pwd)/conf:/conf iz_search

#start landscape service 
docker run -d --net=informiz_net --name=lands_ser --volume=$(pwd)/conf:/conf lands_ser
#start landscape endpoint
docker run -d --net=informiz_net --name=informiz --volume=$(pwd)/conf:/conf -p 8080:8080 lands_rest
