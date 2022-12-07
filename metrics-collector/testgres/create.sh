#!/bin/bash

ip_addr=$1

if [ ! -d /nfs_shared ]; then
  sudo mkdir /nfs_shared
  sudo bash -c "echo \"/nfs_shared $ip_addr/24(rw,sync,no_root_squash)\" >> /etc/exports"
fi

echo "restart nfs-kernel-server"
systemctl restart nfs-kernel-server

#

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

#

if [[ ! $(helm repo list) =~ "bitnami" ]]; then
  echo "add repo bitnami"
  helm repo add bitnami https://charts.bitnami.com/bitnami
fi

if [[ ! $(helm list) =~ "bitnami" ]]; then
  echo "install postgresql"
  helm install postgres bitnami/postgresql -f values.yaml
fi