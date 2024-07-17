#!/bin/bash

if [[ $(helm list) =~ "postgres" ]]; then
  helm delete postgres
  kubectl delete -f pvc.yaml
fi

#

if [[ $(helm list) =~ "nfs-subdir-external-provisioner" ]]; then
  helm delete nfs-subdir-external-provisioner
fi