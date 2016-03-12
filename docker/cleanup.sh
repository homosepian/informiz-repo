#!/bin/bash

docker stop escluster && docker rm escluster
docker stop izagent && docker rm izagent
docker network rm stream_net


