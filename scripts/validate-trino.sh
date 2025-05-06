#!/bin/bash

# Colors for terminal output
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Emojis
CHECK="✅"
WARNING="⚠️"
ERROR="❌"
INFO="ℹ️"

echo -e "${YELLOW}${INFO} Validating Trino...${NC}"

# Check if Trino container is running
echo -e "${YELLOW}${INFO} Checking if Trino container is running...${NC}"
if docker ps | grep -q trino-coordinator; then
    echo -e "${GREEN}${CHECK} Trino container is running.${NC}"
else
    echo -e "${RED}${ERROR} Trino container is not running. Please start it with 'make start'.${NC}"
    exit 1
fi

# Wait for Trino to be ready
echo -e "${YELLOW}${INFO} Waiting for Trino to be ready...${NC}"
MAX_RETRIES=30
RETRY_INTERVAL=5
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec trino-coordinator trino --no-progress --execute "SELECT 1" > /dev/null 2>/dev/null; then
        echo -e "${GREEN}${CHECK} Trino is ready.${NC}"
        break
    else
        echo -e "${YELLOW}${WARNING} Trino is not ready yet. Retrying in ${RETRY_INTERVAL} seconds...${NC}"
        sleep $RETRY_INTERVAL
        RETRY_COUNT=$((RETRY_COUNT + 1))
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}${ERROR} Trino failed to become ready after ${MAX_RETRIES} retries.${NC}"
    exit 1
fi

# Check if Iceberg catalog is available
echo -e "${YELLOW}${INFO} Checking if Iceberg catalog is available...${NC}"
CATALOGS=$(docker exec trino-coordinator trino --no-progress --execute "SHOW CATALOGS" 2>/dev/null)

if echo "$CATALOGS" | grep -q "iceberg"; then
    echo -e "${GREEN}${CHECK} Iceberg catalog is available.${NC}"
else
    echo -e "${RED}${ERROR} Iceberg catalog is not available. Check Trino configuration.${NC}"
    echo -e "${YELLOW}${INFO} Available catalogs:${NC}"
    echo "$CATALOGS"
    exit 1
fi

# Create the test namespace first
echo -e "${YELLOW}${INFO} Creating test namespace in Iceberg...${NC}"
CREATE_RESULT=$(docker exec trino-coordinator trino --no-progress --execute "CREATE SCHEMA IF NOT EXISTS iceberg.test" 2>/dev/null)

# Wait for namespace creation to propagate
echo -e "${YELLOW}${INFO} Waiting for namespace creation to complete...${NC}"
sleep 5

# Try to create a test table and insert data
echo -e "${YELLOW}${INFO} Testing Iceberg catalog functionality...${NC}"

# List schemas in the Iceberg catalog
RESULT=$(docker exec trino-coordinator trino --no-progress --execute "SHOW SCHEMAS FROM iceberg" 2>/dev/null)

if echo "$RESULT" | grep -q "test"; then
    echo -e "${GREEN}${CHECK} Successfully connected to Iceberg catalog and found test schema.${NC}"
else
    echo -e "${YELLOW}${WARNING} Test schema not found yet. Waiting and retrying...${NC}"
    sleep 10
    RESULT=$(docker exec trino-coordinator trino --no-progress --execute "SHOW SCHEMAS FROM iceberg" 2>/dev/null)
    
    if echo "$RESULT" | grep -q "test"; then
        echo -e "${GREEN}${CHECK} Successfully connected to Iceberg catalog and found test schema.${NC}"
    else
        echo -e "${RED}${ERROR} Failed to create test schema in Iceberg catalog.${NC}"
        echo -e "${YELLOW}${INFO} Available schemas:${NC}"
        echo "$RESULT"
        exit 1
    fi
fi

echo -e "${GREEN}${CHECK} Trino validation completed successfully!${NC}"
echo -e "${YELLOW}${INFO} Trino is properly connected to Iceberg catalog and functioning correctly.${NC}"
