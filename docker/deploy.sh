#!/bin/bash

# build images
docker build -t flume -f $(pwd)/flume/Dockerfile $(pwd)/flume
docker build -t elasticsearch -f $(pwd)/elasticsearch/Dockerfile $(pwd)/elasticsearch
docker build -t lands_ser -f $(pwd)/lands-ser/Dockerfile $(pwd)/lands-ser
docker build -t lands_rest -f $(pwd)/lands-rest/Dockerfile $(pwd)/lands-rest
docker build -t iz_graph -f $(pwd)/iz-graph/Dockerfile $(pwd)/iz-graph

# create a bridge network for the app
docker network create informiz_net
# for multiple hosts: docker network create -d overlay informiz_net

# start neo4j. TODO: auth, don't publish port
docker run -d --net=informiz_net --name=neo --volume=$HOME/neo4j/data:/data --volume=$(pwd)/iz-graph/resources:/resources --env=NEO4J_AUTH=none -p=7474:7474 neo4j
# load data to graph
docker run --rm --net=informiz_net --name=iz_graph --volumes-from neo iz_graph

# start elasticsearch
docker run -d --memory-swappiness=0 --net=informiz_net --name=escluster -p 9300:9300 -p 9200:9200 elasticsearch
#start flume
docker run -d --net=informiz_net --name=izagent -p 44444:44444 -t flume

# TODO: run exporter neo4j->elasticsearch

#start landscape service 
docker run -d --net=informiz_net --name=lands_ser lands_ser
#start landscape endpoint
docker run -d --net=informiz_net --name=lands_rest -p 8080:8080 lands_rest
