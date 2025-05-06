#!/bin/bash

# Run the Flight Data Generator application

set -e

# Build the application
echo "Building the application..."
../gradlew build -x test

# Run the application
echo "Running the Flight Data Generator..."
java -jar ../generator/build/libs/event-driven-analytics-ktor-flink-0.1.0-SNAPSHOT.jar "$@"
