#!/bin/bash

if [[ ! $(helm repo list) =~ "amithkk" ]]; then
  echo ""
  echo "add repo amithkk"
  helm repo add amithkk https://amithkk.github.io/stable-diffusion-k8s
  helm repo update
fi

if [[ ! $(helm list) =~ "stable-diffusion" ]]; then
  echo ""
  echo "install stable-diffusion"
  helm install stable-diffusion amithkk/stable-diffusion -f values.yaml
fi
