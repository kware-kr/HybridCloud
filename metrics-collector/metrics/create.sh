#!/bin/bash

ip_addr=$1

if [[ ! $(helm repo list) =~ "bitnami" ]]; then
  echo "add repo bitnami"
  helm repo add bitnami https://charts.bitnami.com/bitnami
fi

if [[ ! $(helm list) =~ "kube-prometheus" ]]; then
  echo "install kube-prometheus"
  kubectl create namespace monitoring
  helm install kube-prometheus bitnami/kube-prometheus --namespace monitoring
fi

echo ""
echo ">> wait for prometheus starting"
sleep 10s

kubectl port-forward --namespace monitoring svc/kube-prometheus-prometheus 9090:9090 --address 0.0.0.0 > /dev/null 2>&1 &
echo $! > .PID_PRMTUS
echo ">> prometheus 9090 started"

kubectl port-forward --namespace monitoring svc/kube-prometheus-node-exporter 9100:9100 --address 0.0.0.0 > /dev/null 2>&1 &
echo $! > .PID_NDEXPR
echo ">> node exporter 9100 started"

###

if [[ ! $(helm list) =~ "grafana" ]]; then
  echo "install grafana"
  if [[ ! $(kubectl get ns) =~ "monitoring" ]]; then
    kubectl create namespace monitoring
  fi
  chmod +x storage.sh
  sudo ./storage.sh create $1
  helm install grafana bitnami/grafana -f values.yaml --namespace monitoring
fi

echo ""
echo ">> wait for grafana starting"
sleep 15s

kubectl port-forward svc/grafana --namespace monitoring --address 0.0.0.0 3000:3000 > /dev/null 2>&1 &
echo $! > .PID_GRFN
echo ">> node grafana 3000 started. wait for running"
