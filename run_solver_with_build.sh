#!/usr/bin/env bash
# build the source code
gradle clean build

# run the jar file with args
java -jar build/libs/prototype-1.0.jar $@