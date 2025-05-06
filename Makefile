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
	@echo "${GREEN}make run-stateless${NC} - Run the Stateless Flink Job locally"
	@echo "${GREEN}make run-stateful${NC} - Run the Stateful Flink Job locally"
	@echo "${GREEN}make deploy-stateless${NC} - Deploy the Stateless Flink Job to the Flink cluster"
	@echo "${GREEN}make deploy-stateful${NC} - Deploy the Stateful Flink Job to the Flink cluster"
	@echo "${GREEN}make list-jobs${NC}    - List running Flink jobs"
	@echo "${GREEN}make cancel-job JOB_ID=<id>${NC} - Cancel a specific Flink job"
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

# Run the Stateless Flink Job locally
.PHONY: run-stateless
run-stateless:
	@echo "${GREEN}${ROCKET} Running the Stateless Flink Job locally...${NC}"
	./gradlew :flink-jobs:runStatelessJob --args="flink-jobs/src/main/resources/application-local.yaml"
	@echo "${GREEN}${CHECK} Stateless Flink Job submitted!${NC}"

# Run the Stateful Flink Job locally
.PHONY: run-stateful
run-stateful: 
	@echo "${GREEN}${ROCKET} Running the Stateful Flink Job locally...${NC}"
	./gradlew :flink-jobs:runStatefulJob --args="flink-jobs/src/main/resources/application-local.yaml"
	@echo "${GREEN}${CHECK} Stateful Flink Job submitted!${NC}"

# Deploy the Stateless Flink Job to the Flink cluster
.PHONY: deploy-stateless
deploy-stateless:
	@echo "${GREEN}${ROCKET} Deploying the Stateless Flink Job to the Flink cluster...${NC}"
	@echo "${BLUE}${INFO} Building the Flink Jobs module...${NC}"
	./gradlew :flink-jobs:shadowJar
	@echo "${BLUE}${INFO} Creating usrlib directory if it doesn't exist...${NC}"
	docker exec -it jobmanager mkdir -p /opt/flink/usrlib || echo "${RED}${ERROR} Failed to create directory${NC}"
	@echo "${BLUE}${INFO} Copying JAR to Flink cluster...${NC}"
	docker cp flink-jobs/build/libs/flink-jobs-0.1.0-SNAPSHOT.jar jobmanager:/opt/flink/usrlib/ || echo "${RED}${ERROR} Failed to copy JAR${NC}"
	@echo "${BLUE}${INFO} Submitting job to Flink cluster...${NC}"
	docker exec -it jobmanager flink run -d -c dev.gamov.flightdemo.flink.StatelessJobKt /opt/flink/usrlib/flink-jobs-0.1.0-SNAPSHOT.jar || echo "${RED}${ERROR} Failed to submit job${NC}"
	@echo "${GREEN}${CHECK} Stateless Flink Job deployed!${NC}"

# Deploy the Stateful Flink Job to the Flink cluster
.PHONY: deploy-stateful
deploy-stateful:
	@echo "${GREEN}${ROCKET} Deploying the Stateful Flink Job to the Flink cluster...${NC}"
	@echo "${BLUE}${INFO} Building the Flink Jobs module...${NC}"
	./gradlew :flink-jobs:shadowJar
	@echo "${BLUE}${INFO} Creating usrlib directory if it doesn't exist...${NC}"
	docker exec -it jobmanager mkdir -p /opt/flink/usrlib || echo "${RED}${ERROR} Failed to create directory${NC}"
	@echo "${BLUE}${INFO} Copying JAR to Flink cluster...${NC}"
	docker cp flink-jobs/build/libs/flink-jobs-0.1.0-SNAPSHOT.jar jobmanager:/opt/flink/usrlib/ || echo "${RED}${ERROR} Failed to copy JAR${NC}"
	@echo "${BLUE}${INFO} Submitting job to Flink cluster...${NC}"
	docker exec -it jobmanager flink run -d -c dev.gamov.flightdemo.flink.StatefulJobKt /opt/flink/usrlib/flink-jobs-0.1.0-SNAPSHOT.jar || echo "${RED}${ERROR} Failed to submit job${NC}"
	@echo "${GREEN}${CHECK} Stateful Flink Job deployed!${NC}"

# List running Flink jobs
.PHONY: list-jobs
list-jobs:
	@echo "${BLUE}${INFO} Listing running Flink jobs...${NC}"
	docker exec -it jobmanager flink list

# Cancel a Flink job
.PHONY: cancel-job
cancel-job:
	@if [ -z "$(JOB_ID)" ]; then \
		echo "${RED}${ERROR} Please provide a JOB_ID: make cancel-job JOB_ID=<id>${NC}"; \
	else \
		echo "${YELLOW}${WARNING} Cancelling Flink job $(JOB_ID)...${NC}"; \
		docker exec -it jobmanager flink cancel $(JOB_ID); \
		echo "${GREEN}${CHECK} Job cancelled!${NC}"; \
	fi
