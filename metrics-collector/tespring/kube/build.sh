#!/bin/bash

cd ..
sudo ./gradlew build jar -Pprofile=dev -Dfile.encoding=UTF-8 \
  && docker build --build-arg JAR_FILE=build/libs/*.jar -t lect/tespring .