#!/bin/bash

# gpu operator

helm uninstall gpu-operator -n monitoring

# prometheus

helm uninstall kube-prometheus -n monitoring

kubectl delete ns monitoring
