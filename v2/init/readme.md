마스터 노드 설치
```sh
1. sudo bash common.sh
2. bash master.sh <ip_address>
3. bash helm.sh
4. bash nfs.sh <ip_address>

워커 노드 설치
1. sudo bash common.sh
2. sudo kubeadm join <master_node_ip_address>:6443 --token <token> --discovery-token-ca-cert-hash <hash> [--node-name node-name]
