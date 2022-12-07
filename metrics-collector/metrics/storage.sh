#!/bin/bash

CMD=$1
ip_addr=$2

if [[ $CMD == "create" ]]; then

  if [[ ! -d /nfs_shared ]]; then
    mkdir /nfs_shared
    bash -c "echo \"/nfs_shared $ip_addr/24(rw,sync,no_root_squash)\" >> /etc/exports"
  fi

  echo "restart nfs-kernel-server"
  systemctl restart nfs-kernel-server

  if [[ ! $(helm repo list) =~ "nfs-subdir-external-provisioner" ]]; then
    echo "add repo nfs-provisioner"
    helm repo add nfs-subdir-external-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
  fi

  if [[ ! $(helm list) =~ "nfs-subdir-external-provisioner" ]]; then
    echo "install nfs-provisioner"
    helm install nfs-subdir-external-provisioner nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
      --set nfs.server=$ip_addr \
      --set nfs.path=/nfs_shared
  fi
fi

if [[ $CMD == "delete" ]]; then
  echo ""
fi