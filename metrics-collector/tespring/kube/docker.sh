sudo kubectl create secret generic docker-config \
    --from-file=.dockerconfigjson="/root/.docker/config.json" \
    --type=kubernetes.io/dockerconfigjson

kubectl get secret docker-config --output=yaml