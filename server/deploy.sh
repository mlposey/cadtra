#!/bin/bash
# Note: This script won't work without access to the project Docker Hub
# and access to the remote server.
#
# This deployment script assumes the remote has the following:
# - Environment variables set for CLIENT_ID and CADTRA_DB_PASSWORD
# - docker and docker-compose
# - The directory ~/cadtra

REMOTE=marcusposey@srv.marcusposey.com

docker login

docker build -t mlposey/cadtra-db -f database/Dockerfile database/
docker push mlposey/cadtra-db

docker build -t mlposey/cadtra-api -f src/Dockerfile src/
docker push mlposey/cadtra-api

docker build -t mlposey/cadtra-docs -f api-docs/Dockerfile api-docs/
docker push mlposey/cadtra-docs

scp docker-compose.yaml $REMOTE:/home/marcusposey/cadtra

ssh $REMOTE << EOF
    cd ~/cadtra
    sudo docker-compose down
    sudo docker-compose pull
    sudo -E docker-compose up -d
EOF