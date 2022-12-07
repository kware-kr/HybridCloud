helm repo add nvidia https://helm.ngc.nvidia.com/nvidia
helm repo update

k label no master nvidia.com/gpu.deploy.operands=false
k label no worker1 nvidia.com/gpu.deploy.operands=false
