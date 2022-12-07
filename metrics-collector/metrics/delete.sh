#!/bin/bash

chmod +x exit_port.sh
./exit_port.sh PID_GRFN "grafana 3000 stopped"
./exit_port.sh PID_PRMTUS "prometheus 9090 stopped"
./exit_port.sh PID_NDEXPR "node exporter 9100 stopped"

if [[ $(helm list --namespace monitoring) =~ "grafana" ]]; then
  helm delete grafana --namespace monitoring
fi

if [[ $(helm list --namespace monitoring) =~ "kube-prometheus" ]]; then
  helm delete kube-prometheus --namespace monitoring
fi

if [[ $(kubectl get ns) =~ "monitoring" ]]; then
  kubectl delete namespace monitoring
fi