#!/bin/bash

ip_addr=$1

sudo ./common.sh
sudo ./master.sh $ip_addr
