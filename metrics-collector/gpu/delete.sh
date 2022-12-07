kubectl delete configmap metrics-config -n gpu-operator

helm delete gpu-operator -n gpu-operator

kubectl delete namespace gpu-operator
