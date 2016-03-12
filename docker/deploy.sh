#!/bin/bash

# build images
docker build -t flume -f $(pwd)/flume/Dockerfile $(pwd)/flume
docker build -t elasticsearch -f $(pwd)/elasticsearch/Dockerfile $(pwd)/elasticsearch

# create a bridge network for streaming events into elasticsearch
docker network create stream_net
# for multiple hosts: docker network create -d overlay stream_net

# start elasticsearch
docker run -d --net=stream_net --name=escluster -p 9300:9300 -p 9200:9200 elasticsearch
#start flume
docker run -d --net=stream_net --name=izagent -p 44444:44444 -t flume
