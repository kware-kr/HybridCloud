#!/bin/bash

cd metrics
chmod +x delete.sh
./delete.sh

cd ../testgres
chmod +x delete.sh
./delete.sh

cd ../tespring
kubectl delete -f .

if [[ $(kubectl get ns) =~ "monitoring" ]]; then
  kubectl delete namespace monitoring
fi

echo ">> delete completed"