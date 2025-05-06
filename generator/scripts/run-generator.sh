#!/bin/bash

# Run the Flight Data Generator application

set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Navigate to the project root directory (two levels up from script location)
PROJECT_ROOT="$( cd "${SCRIPT_DIR}/../.." && pwd )"

# Build the application with shadowJar
echo "Building the application..."
cd "${PROJECT_ROOT}"
"${PROJECT_ROOT}/gradlew" generator:shadowJar

# Run the application
echo "Running the Flight Data Generator..."
java -jar "${PROJECT_ROOT}/generator/build/libs/generator-0.1.0-SNAPSHOT.jar" "$@"
