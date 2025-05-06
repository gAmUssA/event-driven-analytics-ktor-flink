#!/bin/bash

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
until kafka-topics --bootstrap-server kafka:9092 --list > /dev/null 2>&1; do
  echo "Kafka not ready yet, waiting..."
  sleep 5
done
echo "Kafka is ready!"

# Create topics
echo "Creating Kafka topics..."

# Create flights topic with 8 partitions and 1 hour retention
kafka-topics --bootstrap-server kafka:9092 \
  --create \
  --if-not-exists \
  --topic flights \
  --partitions 8 \
  --replication-factor 1 \
  --config retention.ms=3600000

# Create processed_flights topic with 4 partitions and 1 hour retention
kafka-topics --bootstrap-server kafka:9092 \
  --create \
  --if-not-exists \
  --topic processed_flights \
  --partitions 4 \
  --replication-factor 1 \
  --config retention.ms=3600000

echo "Topics created successfully!"

# List all topics
echo "Available topics:"
kafka-topics --bootstrap-server kafka:9092 --list

# Show topic details
echo "Topic details:"
kafka-topics --bootstrap-server kafka:9092 --describe --topic flights
kafka-topics --bootstrap-server kafka:9092 --describe --topic processed_flights