# Makefile for Real-Time Flight Tracking Demo
# This file provides commands to build, run, and test the application

# Colors for terminal output
GREEN := \033[0;32m
YELLOW := \033[0;33m
BLUE := \033[0;34m
RED := \033[0;31m
NC := \033[0m # No Color

# Emojis
ROCKET := 🚀
CHECK := ✅
WARNING := ⚠️
ERROR := ❌
INFO := ℹ️
PLANE := ✈️

# Default target
.PHONY: help
help:
	@echo "${BLUE}${PLANE} Real-Time Flight Tracking Demo ${PLANE}${NC}"
	@echo "${YELLOW}${INFO} Available commands:${NC}"
	@echo ""
	@echo "${GREEN}make start${NC}        - Start all services using Docker Compose"
	@echo "${GREEN}make stop${NC}         - Stop all services"
	@echo "${GREEN}make restart${NC}      - Restart all services"
	@echo "${GREEN}make init${NC}         - Initialize Kafka (run once before first start)"
	@echo "${GREEN}make status${NC}       - Check status of all services"
	@echo "${GREEN}make logs${NC}         - Show logs from all services"
	@echo "${GREEN}make build${NC}        - Build the application"
	@echo "${GREEN}make test${NC}         - Run tests"
	@echo "${GREEN}make clean${NC}        - Clean up resources"
	@echo "${GREEN}make kafka-topics${NC} - List Kafka topics"
	@echo "${GREEN}make kafka-ui${NC}     - Open Kafka UI in browser"
	@echo "${GREEN}make flink-ui${NC}     - Open Flink UI in browser"
	@echo "${GREEN}make trino-ui${NC}     - Open Trino UI in browser"
	@echo "${GREEN}make validate-trino${NC} - Validate Trino connectivity and functionality"
	@echo "${GREEN}make minio-ui${NC}     - Open MinIO UI in browser"
	@echo "${GREEN}make run-generator${NC} - Run the Flight Data Generator"
	@echo ""
	@echo "${YELLOW}${INFO} For more information, see README.md${NC}"

# Start all services
.PHONY: start
start:
	@echo "${GREEN}${ROCKET} Starting all services...${NC}"
	docker-compose up -d
	@echo "${GREEN}${CHECK} Services started successfully!${NC}"
	@echo "${BLUE}${INFO} Kafka UI: http://localhost:8080${NC}"
	@echo "${BLUE}${INFO} Flink UI: http://localhost:8082${NC}"
	@echo "${BLUE}${INFO} Trino UI: http://localhost:8083${NC}"
	@echo "${BLUE}${INFO} MinIO UI: http://localhost:9001${NC}"

# Stop all services
.PHONY: stop
stop:
	@echo "${YELLOW}${WARNING} Stopping all services...${NC}"
	docker-compose down
	@echo "${GREEN}${CHECK} Services stopped successfully!${NC}"

# Restart all services
.PHONY: restart
restart:
	@echo "${YELLOW}${WARNING} Restarting all services...${NC}"
	docker-compose down
	docker-compose up -d
	@echo "${GREEN}${CHECK} Services restarted successfully!${NC}"

# Initialize Kafka (run once before first start)
.PHONY: init
init:
	@echo "${YELLOW}${WARNING} Initializing Kafka for KRaft mode...${NC}"
	@echo "${BLUE}${INFO} This command should be run only once before the first start.${NC}"
	docker-compose up kafka-init
	@echo "${GREEN}${CHECK} Kafka initialized successfully!${NC}"
	@echo "${BLUE}${INFO} You can now run 'make start' to start all services.${NC}"

# Check status of all services
.PHONY: status
status:
	@echo "${BLUE}${INFO} Checking status of all services...${NC}"
	docker-compose ps

# Show logs from all services
.PHONY: logs
logs:
	@echo "${BLUE}${INFO} Showing logs from all services...${NC}"
	docker-compose logs -f

# Build the application
.PHONY: build
build:
	@echo "${GREEN}${ROCKET} Building the application...${NC}"
	./gradlew build
	@echo "${GREEN}${CHECK} Application built successfully!${NC}"

# Run tests
.PHONY: test
test:
	@echo "${GREEN}${ROCKET} Running tests...${NC}"
	./gradlew test
	@echo "${GREEN}${CHECK} Tests completed!${NC}"

# Clean up resources
.PHONY: clean
clean:
	@echo "${YELLOW}${WARNING} Cleaning up resources...${NC}"
	docker-compose down -v
	./gradlew clean
	@echo "${GREEN}${CHECK} Resources cleaned up successfully!${NC}"

# List Kafka topics
.PHONY: kafka-topics
kafka-topics:
	@echo "${BLUE}${INFO} Listing Kafka topics...${NC}"
	docker-compose exec kafka kafka-topics --bootstrap-server kafka:9092 --list

# Open Kafka UI in browser
.PHONY: kafka-ui
kafka-ui:
	@echo "${BLUE}${INFO} Opening Kafka UI in browser...${NC}"
	open http://localhost:8080

# Open Flink UI in browser
.PHONY: flink-ui
flink-ui:
	@echo "${BLUE}${INFO} Opening Flink UI in browser...${NC}"
	open http://localhost:8082

# Open Trino UI in browser
.PHONY: trino-ui
trino-ui:
	@echo "${BLUE}${INFO} Opening Trino UI in browser...${NC}"
	open http://localhost:8083

# Validate Trino connectivity and functionality
.PHONY: validate-trino
validate-trino:
	@echo "${BLUE}${INFO} Validating Trino connectivity and functionality...${NC}"
	./scripts/validate-trino.sh

# Open MinIO UI in browser
.PHONY: minio-ui
minio-ui:
	@echo "${BLUE}${INFO} Opening MinIO UI in browser...${NC}"
	open http://localhost:9001

# Run the Flight Data Generator
.PHONY: run-generator
run-generator:
	@echo "${GREEN}${PLANE} Running the Flight Data Generator...${NC}"
	./generator/scripts/run-generator.sh
	@echo "${GREEN}${CHECK} Flight Data Generator started!${NC}"
