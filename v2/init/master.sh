#!/bin/bash

if ! kubectl; then
  echo ">> run common.sh first"
  exit
fi

ip_addr=$1
if [ -z "$ip_addr" ]; then
  echo ">> ip address required"
  exit
fi

sudo kubeadm init --pod-network-cidr=192.168.0.0/16 --apiserver-advertise-address=$ip_addr --node-name=master
sudo apt-get install -y nfs-kernel-server

sleep 3

mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config
echo "cat $HOME/.kube/config"

kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.24.5/manifests/tigera-operator.yaml
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.24.5/manifests/custom-resources.yaml

echo "source <(kubectl completion bash)" >> ~/.bashrc
echo "alias k=kubectl" >> ~/.bashrc
echo "complete -F __start_kubectl k" >> ~/.bashrc
source ~/.bashrc
