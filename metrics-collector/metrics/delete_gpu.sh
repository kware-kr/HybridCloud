#!/bin/bash

chmod +x exit_port.sh
./exit_port.sh PID_GPUOPR "gpu operator 9400 stopped"

kubectl delete configmap metrics-config -n gpu-operator

helm delete gpu-operator -n gpu-operator

kubectl delete namespace gpu-operator

kubectl label no master nvidia.com/gpu.deploy.operands-
kubectl label no worker1 nvidia.com/gpu.deploy.operands-