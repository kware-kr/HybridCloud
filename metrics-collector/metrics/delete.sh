#!/bin/bash

chmod +x exit_port.sh
./exit_port.sh PID_GRFN "grafana 3000 stopped"
./exit_port.sh PID_PRMTUS "prometheus 9090 stopped"
./exit_port.sh PID_NDEXPR "node exporter 9100 stopped"

chmod +x delete_gpu.sh
./delete_gpu.sh

if [[ $(helm list --namespace monitoring) =~ "monitoring" ]]; then
  helm delete grafana --namespace monitoring
  kubectl delete -f pvc.yaml
fi

if [[ $(helm list --namespace monitoring) =~ "monitoring" ]]; then
  helm delete kube-prometheus --namespace monitoring
fi

if [[ $(kubectl get ns) =~ "monitoring" ]]; then
  kubectl delete namespace monitoring
fi
