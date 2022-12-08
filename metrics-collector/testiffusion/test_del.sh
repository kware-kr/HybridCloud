#!/bin/bash

NAME="PID_DIFFSN"

if [[ -f ".$NAME" ]]; then
  PID=$(cat .$NAME)

  if [[ ! -z ".$NAME" ]]; then
    echo "kill $PID"
    kill -15 $PID
  fi

  echo "remove .$NAME"
  rm .$NAME
  echo "stable diffusion 8080 stopped"
fi