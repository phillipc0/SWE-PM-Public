#!/bin/bash

rm -r SWE-PM.jar 2>/dev/null
rm -r app.log 2>/dev/null

# Stoppe die alte Anwendung, falls sie läuft
sudo kill -9 $(sudo lsof -t -i :8069)

# Starte die neue Version
nohup java -jar /home/linux/deployment/SWE-PM.jar > /home/linux/deployment/app.log 2>&1 &
