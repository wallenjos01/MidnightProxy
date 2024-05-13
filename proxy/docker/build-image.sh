#!/bin/bash

if [ -z "$1" ]
then
  echo "Usage: $0 <image-tag>"
  exit 1
fi

scriptPath=$(readlink -f "$0")
scriptDir=$(dirname "$scriptPath")

cd $scriptDir/../../
./gradlew :proxy:build :proxy:copyFinalJar

cd proxy/docker
make
docker build . -t $1