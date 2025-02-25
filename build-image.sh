#!/bin/bash

set -x

if [ "$#" -eq 0 ]; then
    echo "Usage: ./build-image.sh <version>"
    exit
fi

# Check for second argument, if it is '--no-push' then do not push the image to docker hub.
PUSH="1"
if [ -n "$2" -a "$2" == "--no-push" ]; then
    PUSH="0"
fi

CURDIR=`pwd`
CONFDIR="$CURDIR/config"
FINALPROFDIR="$CURDIR/final-profiles"

#Building the snmp poller service
mvn clean install -DskipTests

python scripts/configPackageBuilder.py $CONFDIR $FINALPROFDIR

echo "Building the  SNMP POLLER  version $1 Image..."
if docker build -t snmp-poller:$1 .; then
    if [ $PUSH -eq "1" ]; then
        echo "Tagging the SNMP POLLER v$1.1 Image..."
        if docker tag snmp-poller:$1.1 snmp-poller:$1.1; then
            echo "Pushing the SNMP POLLER v$1.1 Image..."
            if docker push snmp-poller:$1.1; then
                echo "Pushed the image ..."
            else
                echo "Failed pushing the image..."
            fi
        else
            echo "Failed tagging the SNMP POLLER v$1.1 Image ..."
        fi
    else
	echo "Not pushing the image ..."
    fi
else
    echo "Failed building the SNMP POLLER v$1.1 Image ..."
fi



