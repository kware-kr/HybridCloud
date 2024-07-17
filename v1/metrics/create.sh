#!/bin/bash

chmod +x create_gpu.sh
./create_gpu.sh

###
#helm repostiory 등록
if [[ ! $(helm repo list) =~ "bitnami" ]]; then
  echo ""
  echo "add repo bitnami"
  helm repo add bitnami https://charts.bitnami.com/bitnami
fi

#kube-prometheus 설치
if [[ ! $(helm list) =~ "kube-prometheus" ]]; then
  echo ""
  echo "install kube-prometheus"
  kubectl create namespace monitoring

  echo ""
  echo ">> wait for prometheus starting"

  helm install --wait kube-prometheus bitnami/kube-prometheus --namespace monitoring -f prometheus.yaml
  sleep 10s

  kubectl port-forward --namespace monitoring svc/kube-prometheus-prometheus 9090:9090 --address 0.0.0.0 > /dev/null 2>&1 &
  echo $! > .PID_PRMTUS

  echo ""
  echo ">> prometheus 9090 started"

  kubectl port-forward --namespace monitoring svc/kube-prometheus-node-exporter 9100:9100 --address 0.0.0.0 > /dev/null 2>&1 &
  echo $! > .PID_NDEXPR

  echo ""
  echo ">> node exporter 9100 started"
fi

###

#grafana설치
if [[ ! $(helm list) =~ "grafana" ]]; then
  echo ""
  echo "install grafana"
  if [[ ! $(kubectl get ns) =~ "monitoring" ]]; then
    kubectl create namespace monitoring
  fi

  echo ""
  echo ">> wait for grafana starting"

  kubectl apply -f pvc.yaml
  helm install --wait grafana bitnami/grafana -f grafana.yaml --namespace monitoring
  sleep 10s

  kubectl port-forward svc/grafana --namespace monitoring --address 0.0.0.0 3000:3000 > /dev/null 2>&1 &
  echo $! > .PID_GRFN

  echo ""
  echo ">> grafana 3000 started. wait for running"
fi
