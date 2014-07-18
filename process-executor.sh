#!/bin/bash 
PATH=/bin:/usr/bin:/usr/local/bin
WORK=${PWD}/
cd  "${WORK}${1}/"
javac $2
java $3
