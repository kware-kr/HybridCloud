#!/bin/bash

kubectl create ns monitoring

# prometheus

helm repo add bitnami https://charts.bitnami.com/bitnami
helm install kube-prometheus bitnami/kube-prometheus -n monitoring -f prometheus.yaml

while :
do
  kubectl get svc -n monitoring kube-prometheus-prometheus 2> /dev/null && break
  echo -ne "."
  sleep 3
done

kubectl apply -f prometheus-service.yaml
