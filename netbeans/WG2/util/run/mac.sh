#!/bin/bash
# Get the directory of the SCRIPT and CD to it
DIR="$(cd "$( dirname "$0" )" && pwd)"
echo Moving into $DIR;
cd $DIR;
echo WG2 Starting...
java -jar WG2.jar
