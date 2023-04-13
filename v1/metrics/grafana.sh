#!/bin/bash

CURR_DIR=$(pwd)
cd /nfs_shared/monitoring*
cp $CURR_DIR/grafana_modi.sh grafana_modi.sh
cp $CURR_DIR/grafana_icon.svg grafana_icon.svg
cp $CURR_DIR/fav32.png fav32.png

POD_NAME=$(kubectl get po -n monitoring -o custom-columns="TEST:metadata.name" | grep grafana)
WORKER_NAME=$(kubectl get po -n monitoring -o custom-columns="TEST:spec.nodeName" --field-selector metadata.name=$POD_NAME --no-headers)

echo -e ">> connect SSH to \033[0;33m$WORKER_NAME\033[0m and using the commands:

  sudo docker ps -f name=k8s_grafana -q
  sudo docker exec -it -u root [CONTAINER_ID] /bin/bash
  sh /opt/bitnami/grafana/data/grafana_modi.sh
"