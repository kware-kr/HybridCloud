#!/bin/bash

if ! helm; then
  echo ">> run helm.sh first"
  exit
fi

ip_addr=$1
if [ -z "$ip_addr" ]; then
  echo ">> ip address required"
  exit
fi

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
  master=$(kubectl get node --no-headers -o custom-columns=":metadata.name" -l node-role.kubernetes.io/master)
  kubectl taint node $master node-role.kubernetes.io/master:NoSchedule-

  helm install nfs-subdir-external-provisioner nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
    --set nfs.server=$ip_addr \
    --set nfs.path=/nfs_shared
  
  kubectl taint node $master node-role.kubernetes.io/master:NoSchedule
fi