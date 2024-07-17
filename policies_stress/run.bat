docker volume create lectapi-vol
docker run -d --name lectapi -p 8000:80 -v lectapi-vol://app/data lectmoh/api