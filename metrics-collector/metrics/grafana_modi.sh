#!/bin/bash

cd /opt/bitnami/grafana/public/build
FILE_NAME=$(grep -rl "Welcome to Grafana" *.js)

echo $FILE_NAME
sed -i 's/"AppTitle","Grafana"/"AppTitle","Kware"/' $FILE_NAME
sed -i 's/"LoginTitle","Welcome to Grafana"/"LoginTitle","Kware Cloud Resources Monitoring System"/' $FILE_NAME

cp /opt/bitnami/grafana/data/grafana_icon.svg /opt/bitnami/grafana/public/img/grafana_icon.svg
cp /opt/bitnami/grafana/data/fav32.png /opt/bitnami/grafana/public/img/fav32.png
