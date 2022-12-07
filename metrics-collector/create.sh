#!/bin/bash

ip_addr=$1

cd testgres
chmod +x create.sh
./create.sh $ip_addr

cd ../tespring/kube
chmod +x {build.sh,docker.sh}
kubectl apply -f .

cd ../../metrics
chmod +x create.sh $ip_addr
./create.sh

echo ">> create completed"