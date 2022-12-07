kubectl create namespace gpu-operator

kubectl create configmap metrics-config -n gpu-operator --from-file=dcgm-metrics.csv

helm install --wait gpu-operator \
     -n gpu-operator --create-namespace \
     nvidia/gpu-operator
