마스터 노드 설치
```sh
1. sudo bash update.sh
2. sudo bash common.sh
3. bash master.sh <ip_address>
4. bash helm.sh
5. bash nfs.sh <ip_address>

워커 노드 설치
1. sudo bash update.sh
2. sudo bash common.sh
3. sudo kubeadm join <master_node_ip_address>:6443 --token <token> --discovery-token-ca-cert-hash <hash> [--node-name node-name]

문제: `there is a snap with that name.`
참조: https://github.com/kubernetes/kubernetes/issues/59959