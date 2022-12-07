#!/bin/bash

ip_addr=$1

if [ -z "$ip_addr" ]; then
  echo ">> ip address required"
  exit
fi

cd testgres
chmod +x create.sh
./create.sh $ip_addr

cd ../tespring/kube
# chmod +x {build.sh,docker.sh}
kubectl apply -f .

cd ../../metrics
chmod +x create.sh
./create.sh $ip_addr

echo ">> create completed"