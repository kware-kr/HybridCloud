#!/bin/bash

kubectl create ns monitoring

# gpu operator

helm repo add nvidia https://helm.ngc.nvidia.com/nvidia

echo ">> Wait for starting gpu-operator"
helm install --wait gpu-operator -n monitoring --create-namespace nvidia/gpu-operator --set driver.enabled=false

while :
do
  kubectl get svc -n monitoring nvidia-dcgm-exporter 2> /dev/null && break
  echo -ne "."
  sleep 5
done

kubectl patch svc -n monitoring nvidia-dcgm-exporter --patch='{"spec":{"type":"NodePort","ports":[{"port":9400,"targetPort":9400,"nodePort":30094}]}}'

# prometheus

helm repo add bitnami https://charts.bitnami.com/bitnami
helm install kube-prometheus bitnami/kube-prometheus -n monitoring -f prometheus.yaml

while :
do
  kubectl get svc -n monitoring kube-prometheus-prometheus 2> /dev/null && break
  echo -ne "."
  sleep 5
done

kubectl apply -f prometheus-service.yaml
