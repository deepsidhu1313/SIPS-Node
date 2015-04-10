#!/bin/bash 
PATH=/bin:/usr/bin:/usr/local/bin
WORK=${PWD}/
cd  "${WORK}${1}/"
bash ${WORK}ant/bin/ant -Darg1=$2
