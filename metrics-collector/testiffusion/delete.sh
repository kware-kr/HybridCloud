#!/bin/bash

if [[ $(helm list) =~ "stable-diffusion" ]]; then
  helm delete stable-diffusion
fi
