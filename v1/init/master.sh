#!/bin/bash

ip_addr=$1

sudo kubeadm init --pod-network-cidr=192.168.0.0/16 --apiserver-advertise-address=$ip_addr
sudo apt-get install -y nfs-kernel-server

mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config

kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.24.5/manifests/tigera-operator.yaml
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.24.5/manifests/custom-resources.yaml

# kubectl taint nodes --all node-role.kubernetes.io/control-plane- node-role.kubernetes.io/master-
# node/<your-hostname> untainted

source <(kubectl completion bash)
echo "source <(kubectl completion bash)" >> ~/.bashrc

echo 'alias k=kubectl' >> ~/.bashrc
echo 'complete -F __start_kubectl k' >> ~/.bashrc

source ~/.bashrc