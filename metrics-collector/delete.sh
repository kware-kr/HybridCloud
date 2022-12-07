#!/bin/bash

cd metrics
chmod +x delete.sh
./delete.sh

cd ../testgres
chmod +x delete.sh
./delete.sh

cd ../tespring/kube
kubectl delete -f .

echo ">> delete completed"