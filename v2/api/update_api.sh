#!/bin/bash

master=$(kubectl get node --no-headers -o custom-columns=":metadata.name" -l node-role.kubernetes.io/master)
kubectl taint node $master node-role.kubernetes.io/master:NoSchedule-
kubectl patch deployment -n api lectapi -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"date\":\"`date +'%s'`\"}}}}}"
sleep 3s
kubectl taint node $master node-role.kubernetes.io/master:NoSchedule
