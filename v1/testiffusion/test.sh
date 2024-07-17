#!/bin/bash

POD_NAME=$(kubectl get pods --namespace default -l "app.kubernetes.io/name=stable-diffusion,app.kubernetes.io/instance=stable-diffusion" -o jsonpath="{.items[0].metadata.name}")
CONTAINER_PORT=$(kubectl get pod --namespace default $POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}")

kubectl port-forward $POD_NAME 8080:$CONTAINER_PORT --address 0.0.0.0 > /dev/null 2>&1 &
echo $! > .PID_DIFFSN

echo ">> stable diffusion 8080 started. wait for running"
