#!/bin/bash

if [[ ! $(helm repo list) =~ "nvidia" ]]; then
  echo ""
  echo "add repo nvidia"
  helm repo add nvidia https://helm.ngc.nvidia.com/nvidia
fi

kubectl label no master nvidia.com/gpu.deploy.operands=false
kubectl label no worker1 nvidia.com/gpu.deploy.operands=false

kubectl create namespace gpu-operator

echo ""
echo ">> wait for gpu-operator starting"

helm install --wait gpu-operator \
     -n gpu-operator --create-namespace \
     nvidia/gpu-operator

sleep 10s

kubectl -n gpu-operator port-forward service/nvidia-dcgm-exporter 9400:9400 --address 0.0.0.0 > /dev/null 2>&1 &
echo $! > .PID_GPUOPR

echo ""
echo ">> gpu operator 9400 started. wait for running"
