#!/bin/bash

docker stop escluster && docker rm escluster
docker stop izagent && docker rm izagent
docker stop lands_rest && docker rm lands_rest
docker stop lands_ser && docker rm lands_ser
docker network rm informiz_net


